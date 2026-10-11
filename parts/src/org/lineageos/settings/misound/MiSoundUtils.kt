/*
 * Copyright (C) 2018,2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.misound

import android.content.Context
import android.media.*
import android.media.audiofx.AudioEffect
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemProperties
import android.util.Log
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.lineageos.settings.utils.PartsPreferences

data class MiSoundState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    val equalizer: Boolean = true,
    val pauseDuringCalls: Boolean = true,
    val paused: Boolean = false,
    val headset: Int = 0,
    val preset: String = "0,0,0,0,0,0,0",
    val scenario: Int = 4,
    val hifi: Boolean = false,
    val supportedHeadsets: List<Int> = emptyList(),
)

/** Persistent process-wide owner: activities never own or release the DSP effect. */
class MiSoundUtils private constructor(context: Context) {
    companion object {
        const val PREF_ENABLE = "misound_enable"
        const val PREF_HEADSET = "misound_headset_pref"
        const val PREF_PRESET = "misound_preset_pref"
        const val PREF_SCENE = "scenario_selection"
        const val PREF_HIFI = "misound_hifi_pref"
        const val PREF_EQ = "misound_eq_enabled"
        const val PREF_PAUSE = "misound_pause_during_calls"
        private const val FLAT = "0,0,0,0,0,0,0"
        private var instance: MiSoundUtils? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): MiSoundUtils =
            instance ?: MiSoundUtils(context).also { instance = it }

        private fun parseLevels(preset: String): FloatArray {
            val values = preset.split(',')
            require(values.size == 7) { "Expected seven MiSound bands" }
            return values
                .map { it.trim().toFloat().also { level -> require(level.isFinite()) } }
                .toFloatArray()
        }

        private fun cleanup(failure: RuntimeException, action: () -> Unit) {
            try {
                action()
            } catch (error: RuntimeException) {
                if (failure !== error) failure.addSuppressed(error)
            }
        }
    }

    private val app = context.applicationContext.createDeviceProtectedStorageContext()
    private val preferences = PartsPreferences.getDefaultSharedPreferences(app)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val worker = HandlerThread("MiSound").apply { start() }
    private val handler = Handler(worker.looper)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executor { handler.post(it) }
    private val mutableState = MutableStateFlow(MiSoundState())
    val state = mutableState.asStateFlow()
    private var supportedHeadsets = emptyList<Int>()
    private var callbacksRegistered = false
    private var callbackAttempts = 0
    private val callbackTask = Runnable { registerCallbacks() }
    private var serverDown = false
    private var communicationActive = false
    private var appliedEnabled: Boolean? = null
    private var sound: MiSoundEffect? = null
    private var attempts = 0
    private val restoreTask = Runnable { restore() }
    private val audioTask = Runnable { updateAudioState() }
    private val modeListener = AudioManager.OnModeChangedListener { scheduleAudioUpdate() }
    private val playback =
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
                scheduleAudioUpdate()
            }
        }
    private val recording =
        object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(
                configs: MutableList<AudioRecordingConfiguration>
            ) {
                scheduleAudioUpdate()
            }
        }

    /** Move legacy audio keys without overwriting values already saved by this version. */
    private fun migratePreferences() {
        val saved = preferences.all
        val editor = preferences.edit()
        var changed = false
        for (key in listOf(PREF_ENABLE, PREF_HEADSET, PREF_PRESET, PREF_HIFI, PREF_EQ, PREF_PAUSE)) {
            val oldKey = key.replaceFirst("misound_", "dirac_")
            if (!saved.containsKey(oldKey)) continue
            if (!saved.containsKey(key)) {
                when (val value = saved[oldKey]) {
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                }
            }
            editor.remove(oldKey)
            changed = true
        }
        if (changed) editor.apply()
    }

    init {
        handler.post(callbackTask)
    }

    private fun registerCallbacks() {
        migratePreferences()
        try {
            audio.setAudioServerStateCallback(
                executor,
                object : AudioManager.AudioServerStateCallback() {
                    override fun onAudioServerDown() =
                        synchronized(this@MiSoundUtils) {
                            serverDown = true
                            handler.removeCallbacks(restoreTask)
                            handler.removeCallbacks(audioTask)
                            releaseEffect()
                            publishState()
                        }

                    override fun onAudioServerUp() =
                        synchronized(this@MiSoundUtils) {
                            serverDown = false
                            attempts = 0
                            restore()
                        }
                },
            )
            audio.addOnModeChangedListener(executor, modeListener)
            audio.registerAudioPlaybackCallback(playback, handler)
            audio.registerAudioRecordingCallback(recording, handler)
            callbacksRegistered = true
        } catch (failure: RuntimeException) {
            cleanup(failure) { audio.removeOnModeChangedListener(modeListener) }
            cleanup(failure) { audio.unregisterAudioPlaybackCallback(playback) }
            cleanup(failure) { audio.unregisterAudioRecordingCallback(recording) }
            cleanup(failure) { audio.clearAudioServerStateCallback() }
            handler.removeCallbacks(audioTask)
            Log.w("MiSoundUtils", "Cannot register audio callbacks", failure)
            if (++callbackAttempts < 5) handler.postDelayed(callbackTask, 1000)
            releaseEffect()
            publishState()
            return
        }
        restore()
    }

    @Synchronized
    private fun scheduleAudioUpdate() {
        handler.removeCallbacks(audioTask)
        handler.post(audioTask)
    }

    private fun effectiveEnabled(requested: Boolean, pause: Boolean): Boolean {
        communicationActive =
            audio.mode != AudioManager.MODE_NORMAL && audio.mode != AudioManager.MODE_RINGTONE
        if (!communicationActive)
            communicationActive =
                audio.activePlaybackConfigurations.any {
                    it.playerState == AudioPlaybackConfiguration.PLAYER_STATE_STARTED &&
                        it.audioAttributes.usage == AudioAttributes.USAGE_VOICE_COMMUNICATION
                }
        if (!communicationActive)
            communicationActive =
                audio.activeRecordingConfigurations.any {
                    !it.isClientSilenced &&
                        it.clientAudioSource == MediaRecorder.AudioSource.VOICE_COMMUNICATION
                }
        return requested && !(pause && communicationActive)
    }

    @Synchronized
    private fun updateAudioState() {
        if (serverDown || (sound == null && attempts >= 5)) return
        try {
            requireEffect()
            val enabled = effectiveEnabled(isEnabledRequested(), isPauseDuringCallsEnabled())
            if (appliedEnabled != enabled) {
                applyEnabled(enabled)
                publishState()
            }
        } catch (error: RuntimeException) {
            Log.w("MiSoundUtils", "Cannot update communication bypass", error)
            restoreAfterFailure()
        }
    }

    @Synchronized
    private fun restore() {
        try {
            requireEffect()
            attempts = 0
            handler.removeCallbacks(restoreTask)
            publishState()
        } catch (error: RuntimeException) {
            Log.w("MiSoundUtils", "Cannot restore MiSound", error)
            if (++attempts < 5 && !serverDown) {
                handler.removeCallbacks(restoreTask)
                handler.postDelayed(restoreTask, 1000)
            }
            publishState()
        }
    }

    fun requestRefresh() {
        handler.post {
            attempts = 0
            if (callbacksRegistered) restore()
            else {
                callbackAttempts = 0
                handler.removeCallbacks(callbackTask)
                registerCallbacks()
            }
        }
    }

    /** Queue native operations in user order; neither UI nor tile threads wait for the DSP. */
    fun submit(action: (MiSoundUtils) -> Unit, onFailure: () -> Unit = {}) {
        handler.post {
            try {
                action(this)
            } catch (error: RuntimeException) {
                Log.w("MiSoundUtils", "Cannot apply MiSound", error)
                restoreAfterFailure()
                mainHandler.post(onFailure)
            }
            publishState()
        }
    }

    private fun publishState() {
        mutableState.value = MiSoundState(
            available = !serverDown && sound?.hasControl() == true,
            enabled = isEnabledRequested(),
            equalizer = isEqualizerEnabled(),
            pauseDuringCalls = isPauseDuringCallsEnabled(),
            paused = isPausedForCommunication(),
            headset = getHeadsetType(),
            preset = getSavedPreset(),
            scenario = getScenario(),
            hifi = getHifiMode(),
            supportedHeadsets = supportedHeadsets,
        )
    }

    private fun releaseEffect() {
        val previous = sound
        sound = null
        supportedHeadsets = emptyList()
        appliedEnabled = null
        if (previous == null) return
        try {
            if (!serverDown && previous.hasControl())
                try {
                    previous.setEnabled(false)
                } finally {
                    previous.setMusic(0)
                }
        } catch (error: RuntimeException) {
            Log.w("MiSoundUtils", "Cannot bypass MiSound before release", error)
        } finally {
            try {
                previous.release()
            } catch (error: RuntimeException) {
                Log.w("MiSoundUtils", "Cannot release MiSound", error)
            }
        }
    }

    private fun requireEffect(): MiSoundEffect {
        check(callbacksRegistered) { "Audio callbacks unavailable" }
        check(!serverDown) { "Audio server is down" }
        try {
            sound?.let { if (it.hasControl()) return it }
            releaseEffect()
            val effect = MiSoundEffect(0, 0)
            sound = effect
            check(effect.hasControl()) { "MiSound control unavailable" }
            applyEnabled(false)
            if (isEnabledRequested()) {
                applySettings()
                applyEnabled(effectiveEnabled(true, isPauseDuringCallsEnabled()))
            }
            supportedHeadsets = try {
                effect.getHeadsetList().toList()
            } catch (error: RuntimeException) {
                Log.w("MiSoundUtils", "Headset capabilities unavailable", error)
                emptyList()
            }
            return effect
        } catch (error: RuntimeException) {
            releaseEffect()
            throw error
        }
    }

    private fun safeString(key: String, fallback: String): String =
        try {
            preferences.getString(key, fallback) ?: fallback
        } catch (_: ClassCastException) {
            preferences.edit().remove(key).apply()
            fallback
        }

    private fun safeBoolean(key: String, fallback: Boolean): Boolean =
        try {
            preferences.getBoolean(key, fallback)
        } catch (_: ClassCastException) {
            preferences.edit().remove(key).apply()
            fallback
        }

    private fun safeInt(key: String, fallback: Int, range: IntRange): Int {
        val parsed = safeString(key, fallback.toString()).toIntOrNull()
        if (parsed != null && parsed in range) return parsed
        preferences.edit().remove(key).apply()
        return fallback
    }

    @Synchronized fun getHeadsetType() = safeInt(PREF_HEADSET, 0, 0..255)

    @Synchronized fun getScenario() = safeInt(PREF_SCENE, 4, 0..4)

    @Synchronized
    fun getSavedPreset(): String {
        val preset = safeString(PREF_PRESET, FLAT)
        return try {
            parseLevels(preset)
            preset
        } catch (error: IllegalArgumentException) {
            Log.w("MiSoundUtils", "Discarding invalid saved preset", error)
            preferences.edit().remove(PREF_PRESET).apply()
            FLAT
        }
    }

    private fun applySettings() {
        sound!!.setHeadsetType(getHeadsetType())
        applyLevel(getSavedPreset(), isEqualizerEnabled())
        sound!!.setScenario(getScenario())
        applyHifi(getHifiMode())
    }

    private fun applyEnabled(enable: Boolean) {
        val effect = checkNotNull(sound)
        if (enable) {
            effect.setMusic(1)
            check(effect.setEnabled(true) == AudioEffect.SUCCESS)
        } else {
            var failure: RuntimeException? = null
            try {
                check(effect.setEnabled(false) == AudioEffect.SUCCESS)
            } catch (error: RuntimeException) {
                failure = error
            }
            try {
                effect.setMusic(0)
            } catch (error: RuntimeException) {
                if (failure == null) failure = error
                else if (failure !== error) failure.addSuppressed(error)
            }
            failure?.let { throw it }
        }
        check(effect.hasExpectedEnabledState(enable) && effect.getMusic() == if (enable) 1 else 0) {
            "MiSound enable state did not apply"
        }
        appliedEnabled = enable
    }

    @Synchronized
    fun restoreAfterFailure() {
        releaseEffect()
        attempts = 0
        restore()
    }

    @Synchronized fun isEnabledRequested() = safeBoolean(PREF_ENABLE, false)

    @Synchronized fun isEqualizerEnabled() = safeBoolean(PREF_EQ, true)

    @Synchronized fun isPauseDuringCallsEnabled() = safeBoolean(PREF_PAUSE, true)

    @Synchronized
    fun isPausedForCommunication() =
        isEnabledRequested() && isPauseDuringCallsEnabled() && communicationActive

    @Synchronized
    fun setEnabled(enable: Boolean): Boolean {
        if (!enable) preferences.edit().putBoolean(PREF_ENABLE, false).apply()
        return try {
            requireEffect()
            applyEnabled(false)
            if (enable) {
                applySettings()
                applyEnabled(effectiveEnabled(true, isPauseDuringCallsEnabled()))
            }
            preferences.edit().putBoolean(PREF_ENABLE, enable).apply()
            true
        } catch (error: RuntimeException) {
            Log.w("MiSoundUtils", "Cannot change MiSound", error)
            false
        }
    }

    @Synchronized
    fun setEqualizerEnabled(enabled: Boolean) {
        requireEffect()
        applyLevel(getSavedPreset(), enabled)
        preferences.edit().putBoolean(PREF_EQ, enabled).apply()
    }

    @Synchronized
    fun setPauseDuringCallsEnabled(enabled: Boolean) {
        requireEffect()
        applyEnabled(effectiveEnabled(isEnabledRequested(), enabled))
        preferences.edit().putBoolean(PREF_PAUSE, enabled).apply()
    }

    @Synchronized
    fun setLevel(preset: String) {
        parseLevels(preset)
        requireEffect()
        applyLevel(preset, isEqualizerEnabled())
        preferences.edit().putString(PREF_PRESET, preset).apply()
    }

    private fun applyLevel(preset: String, enabled: Boolean) {
        val levels = if (enabled) parseLevels(preset) else FloatArray(0)
        repeat(MiSoundEffect.EQ_BAND_COUNT) { sound!!.setLevel(it, levels.getOrElse(it) { 0f }) }
    }

    @Synchronized
    fun setHeadsetType(value: Int) {
        requireEffect().setHeadsetType(value)
        preferences.edit().putString(PREF_HEADSET, value.toString()).apply()
    }

    @Synchronized fun getHifiMode() = safeBoolean(PREF_HIFI, false)

    fun isHifiSupported() =
        SystemProperties.getBoolean("vendor.audio.feature.hifi_audio.enable", false)

    private fun applyHifi(enabled: Boolean) {
        if (isHifiSupported()) {
            sound!!.setHifiMode(if (enabled) 1 else 0)
            audio.setParameters("hifi_mode=$enabled")
        }
    }

    @Synchronized
    fun setHifiMode(value: Int) {
        check(isHifiSupported()) { "HAL Hi-Fi feature is disabled" }
        require(value in 0..1)
        requireEffect()
        applyHifi(value != 0)
        preferences.edit().putBoolean(PREF_HIFI, value != 0).apply()
    }

    @Synchronized
    fun setScenario(value: Int) {
        requireEffect().setScenario(value)
        preferences.edit().putString(PREF_SCENE, value.toString()).apply()
    }
}
