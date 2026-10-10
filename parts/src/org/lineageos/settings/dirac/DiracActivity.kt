/*
 * Copyright (C) 2018 The LineageOS Project
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

package org.lineageos.settings.dirac

import android.media.*
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.lineageos.settings.R
import org.lineageos.settings.compose.*

class DiracActivity : PartsActivity() {
    @Composable
    override fun Content() {
        var owner by remember { mutableStateOf<DiracUtils?>(null) }
        var revision by remember { mutableIntStateOf(0) }
        var headphones by remember { mutableStateOf(false) }
        val lifecycle = LocalLifecycleOwner.current
        DisposableEffect(lifecycle) {
            val audio = getSystemService(AudioManager::class.java)
            val media = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
            var registered = false
            val stateListener = Runnable { revision++ }
            fun route(devices: List<AudioDeviceAttributes>) {
                headphones =
                    devices.any {
                        it.type in
                            setOf(
                                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                                AudioDeviceInfo.TYPE_USB_HEADSET,
                                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                                AudioDeviceInfo.TYPE_BLE_HEADSET,
                            )
                    }
            }
            val listener =
                AudioManager.OnDevicesForAttributesChangedListener { _, devices ->
                    route(devices)
                    revision++
                }
            fun stop() {
                if (registered)
                    try {
                        audio.removeOnDevicesForAttributesChangedListener(listener)
                    } catch (error: RuntimeException) {
                        Log.w("DiracActivity", "Cannot unregister route", error)
                    }
                registered = false
                owner?.removeListener(stateListener)
            }
            fun start() {
                stop()
                try {
                    owner = DiracUtils.getInstance(this@DiracActivity)
                    owner?.addListener(stateListener)
                } catch (error: RuntimeException) {
                    Log.w("DiracActivity", "Cannot initialize MiSound", error)
                }
                try {
                    route(audio.getDevicesForAttributes(media))
                    audio.addOnDevicesForAttributesChangedListener(media, mainExecutor, listener)
                    registered = true
                } catch (error: RuntimeException) {
                    headphones = false
                    Log.w("DiracActivity", "Cannot query media route", error)
                }
                revision++
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> start()
                    Lifecycle.Event.ON_PAUSE -> stop()
                    else -> Unit
                }
            }
            lifecycle.lifecycle.addObserver(observer)
            if (lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()
            onDispose {
                lifecycle.lifecycle.removeObserver(observer)
                stop()
            }
        }
        val utils = owner
        // Each callback invalidates the snapshot; the process owner is authoritative.
        val available = remember(utils, revision) { utils?.isAvailable() == true }
        val requested = remember(utils, revision) { utils?.isEnabledRequested() == true }
        val eq = remember(utils, revision) { utils?.isEqualizerEnabled() == true }
        fun apply(action: (DiracUtils) -> Unit) {
            val current = utils ?: return
            try {
                action(current)
            } catch (error: RuntimeException) {
                Log.w("DiracActivity", "Cannot apply MiSound", error)
                Toast.makeText(this, R.string.dirac_apply_failed, Toast.LENGTH_SHORT).show()
                current.restoreAfterFailure()
            }
            revision++
        }
        val hifiSupported = utils?.isHifiSupported() == true
        val headsetOffset = if (headphones) 1 else 0
        val secondaryRowCount = 4 + headsetOffset + (if (hifiSupported) 1 else 0)
        PartsPage(stringResource(R.string.dirac_title), { finish() }) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    MiSoundIcons.logo,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    "MiSound",
                    style = MaterialTheme.typography.headlineLarge,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ToggleCard(
                stringResource(R.string.dirac_enable),
                when {
                    !available -> stringResource(R.string.dirac_unavailable)
                    utils?.isPausedForCommunication() == true ->
                        stringResource(R.string.dirac_paused_for_calls)
                    else -> stringResource(R.string.dirac_summary)
                },
                requested,
                available || requested,
                main = true,
            ) { enabled ->
                apply { check(it.setEnabled(enabled)) }
            }
            PreferenceRow(0, secondaryRowCount) {
                ToggleCard(
                    stringResource(R.string.dirac_pause_calls_title),
                    stringResource(R.string.dirac_pause_calls_summary),
                    utils?.isPauseDuringCallsEnabled() == true,
                    available,
                ) { enabled ->
                    apply { it.setPauseDuringCallsEnabled(enabled) }
                }
            }
            if (headphones) {
                val catalog =
                    ResourceChoices(
                        R.array.dirac_headset_pref_entries,
                        R.array.dirac_headset_pref_values,
                    )
                val choices =
                    remember(utils, revision, catalog) {
                        try {
                            val supported =
                                if (available) utils?.getSupportedHeadsets()?.toSet().orEmpty()
                                else emptySet()
                            catalog
                                .filter { it.first.toIntOrNull() in supported }
                                .ifEmpty { catalog }
                        } catch (_: RuntimeException) {
                            catalog
                        }
                    }
                PreferenceRow(1, secondaryRowCount) {
                    ChoiceCard(
                        stringResource(R.string.dirac_headset_title),
                        utils?.getHeadsetType()?.toString() ?: "0",
                        choices,
                        available && requested,
                        choiceIcon = { id, selected ->
                            ChoiceIcon(
                                when (id.toIntOrNull()) {
                                    15,
                                    16,
                                    17 -> DialogSymbols.headphones
                                    26,
                                    27 -> DialogSymbols.bluetooth
                                    else -> DialogSymbols.earbuds
                                },
                                selected,
                            )
                        },
                    ) { value ->
                        apply { it.setHeadsetType(value.toInt()) }
                    }
                }
            }
            PreferenceRow(1 + headsetOffset, secondaryRowCount) {
                ToggleCard(
                    stringResource(R.string.dirac_eq_enabled_title),
                    stringResource(R.string.dirac_eq_enabled_summary),
                    eq,
                    available && requested,
                ) { enabled ->
                    apply { it.setEqualizerEnabled(enabled) }
                }
            }
            val presetChoices =
                ResourceChoices(R.array.dirac_preset_pref_entries, R.array.dirac_preset_pref_values)
            val presetSymbols =
                listOf(
                    MiSoundIcons.sliders_vertical,
                    MiSoundIcons.electric_guitar,
                    MiSoundIcons.saxophone,
                    MiSoundIcons.disc_3,
                    MiSoundIcons.piano,
                    MiSoundIcons.mic_vocal,
                    MiSoundIcons.harmonica,
                    MiSoundIcons.audio_lines,
                    MiSoundIcons.guitar,
                    MiSoundIcons.disco_ball,
                    MiSoundIcons.hand_metal,
                    MiSoundIcons.bass_booster,
                    MiSoundIcons.scale,
                    MiSoundIcons.mic,
                    MiSoundIcons.bass_reduction,
                    MiSoundIcons.treble_reduction,
                    MiSoundIcons.soft_bass,
                    MiSoundIcons.soft_treble,
                )
            val presetIcons =
                presetChoices
                    .mapIndexed { index, choice -> choice.first to presetSymbols[index] }
                    .toMap()
            PreferenceRow(2 + headsetOffset, secondaryRowCount) {
                ChoiceCard(
                    stringResource(R.string.dirac_preset_title),
                    utils?.getSavedPreset() ?: "0,0,0,0,0,0,0",
                    presetChoices,
                    available && requested && eq,
                    choiceIcon = { id, selected ->
                        ChoiceIcon(presetIcons[id] ?: MiSoundIcons.sliders_vertical, selected)
                    },
                ) { value ->
                    apply { it.setLevel(value) }
                }
            }
            PreferenceRow(3 + headsetOffset, secondaryRowCount) {
                ChoiceCard(
                    stringResource(R.string.music_headset_scenario_select),
                    utils?.getScenario()?.toString() ?: "4",
                    ResourceChoices(
                        R.array.scenario_selector_titles,
                        R.array.scenario_selector_values,
                    ),
                    available && requested,
                    choiceIcon = { id, selected ->
                        ChoiceIcon(
                            when (id) {
                                "1" -> DialogSymbols.music_note
                                "2" -> DialogSymbols.movie
                                "3" -> DialogSymbols.mic
                                else -> DialogSymbols.auto_awesome
                            },
                            selected,
                        )
                    },
                ) { value ->
                    apply { it.setScenario(value.toInt()) }
                }
            }
            if (hifiSupported)
                PreferenceRow(4 + headsetOffset, secondaryRowCount) {
                    ToggleCard(
                        stringResource(R.string.dirac_hifi_title),
                        stringResource(R.string.dirac_hifi_summary),
                        utils?.getHifiMode() == true,
                        available && requested,
                    ) { enabled ->
                        apply { it.setHifiMode(if (enabled) 1 else 0) }
                    }
                }
        }
    }
}
