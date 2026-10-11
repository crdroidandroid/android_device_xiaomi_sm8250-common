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

package org.lineageos.settings.misound

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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import org.lineageos.settings.R
import org.lineageos.settings.compose.*

class MiSoundActivity : PartsActivity() {
    @Composable
    override fun Content() {
        val utils = remember { MiSoundUtils.getInstance(applicationContext) }
        val state by utils.state.collectAsStateWithLifecycle()
        var headphones by remember { mutableStateOf(false) }
        val lifecycle = LocalLifecycleOwner.current
        LaunchedEffect(lifecycle) {
            val audio = getSystemService(AudioManager::class.java)
            val media = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
            fun route(devices: List<AudioDeviceAttributes>) {
                headphones = devices.any {
                    it.type in setOf(
                        AudioDeviceInfo.TYPE_WIRED_HEADSET,
                        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                        AudioDeviceInfo.TYPE_USB_HEADSET,
                        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                        AudioDeviceInfo.TYPE_BLE_HEADSET,
                    )
                }
            }
            val listener = AudioManager.OnDevicesForAttributesChangedListener { _, devices ->
                route(devices)
            }
            lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                utils.requestRefresh()
                try {
                    withContext(Dispatchers.IO) {
                        audio.addOnDevicesForAttributesChangedListener(media, mainExecutor, listener)
                    }
                    route(withContext(Dispatchers.IO) { audio.getDevicesForAttributes(media) })
                    awaitCancellation()
                } catch (error: RuntimeException) {
                    headphones = false
                    Log.w("MiSoundActivity", "Cannot query media route", error)
                } finally {
                    withContext(NonCancellable + Dispatchers.IO) {
                        try {
                            audio.removeOnDevicesForAttributesChangedListener(listener)
                        } catch (error: RuntimeException) {
                            Log.w("MiSoundActivity", "Cannot unregister route", error)
                        }
                    }
                }
            }
        }
        val available = state.available
        val requested = state.enabled
        val eq = state.equalizer
        fun apply(action: (MiSoundUtils) -> Unit) {
            val app = applicationContext
            utils.submit(action) {
                Toast.makeText(app, R.string.misound_apply_failed, Toast.LENGTH_SHORT).show()
            }
        }
        val hifiSupported = utils.isHifiSupported()
        val headsetOffset = if (headphones) 1 else 0
        val secondaryRowCount = 4 + headsetOffset + (if (hifiSupported) 1 else 0)
        PartsPage(stringResource(R.string.misound_title), { finish() }) {
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
                stringResource(R.string.misound_enable),
                when {
                    !available -> stringResource(R.string.misound_unavailable)
                    state.paused ->
                        stringResource(R.string.misound_paused_for_calls)
                    else -> stringResource(R.string.misound_summary)
                },
                requested,
                available || requested,
                main = true,
            ) { enabled ->
                apply { check(it.setEnabled(enabled)) }
            }
            PreferenceRow(0, secondaryRowCount) {
                ToggleCard(
                    stringResource(R.string.misound_pause_calls_title),
                    stringResource(R.string.misound_pause_calls_summary),
                    state.pauseDuringCalls,
                    available,
                ) { enabled ->
                    apply { it.setPauseDuringCallsEnabled(enabled) }
                }
            }
            if (headphones) {
                val catalog =
                    ResourceChoices(
                        R.array.misound_headset_pref_entries,
                        R.array.misound_headset_pref_values,
                    )
                val choices = remember(catalog, state.supportedHeadsets) {
                    val supported = state.supportedHeadsets.toSet()
                    catalog.filter { it.first.toIntOrNull() in supported }.ifEmpty { catalog }
                }
                PreferenceRow(1, secondaryRowCount) {
                    ChoiceCard(
                        stringResource(R.string.misound_headset_title),
                        state.headset.toString(),
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
                    stringResource(R.string.misound_eq_enabled_title),
                    stringResource(R.string.misound_eq_enabled_summary),
                    eq,
                    available && requested,
                ) { enabled ->
                    apply { it.setEqualizerEnabled(enabled) }
                }
            }
            val presetChoices =
                ResourceChoices(R.array.misound_preset_pref_entries, R.array.misound_preset_pref_values)
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
                    stringResource(R.string.misound_preset_title),
                    state.preset,
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
                    state.scenario.toString(),
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
                        stringResource(R.string.misound_hifi_title),
                        stringResource(R.string.misound_hifi_summary),
                        state.hifi,
                        available && requested,
                    ) { enabled ->
                        apply { it.setHifiMode(if (enabled) 1 else 0) }
                    }
                }
        }
    }
}
