/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.thermal

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import org.lineageos.settings.R
import org.lineageos.settings.compose.*
import org.lineageos.settings.touchsampling.TouchSamplingSettingsActivity

@Composable
private fun LevelCard(title: String, summary: String, value: Int, max: Int, change: (Int) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val interactions = remember { MutableInteractionSource() }
    val dragged by interactions.collectIsDraggedAsState()
    val pressed by interactions.collectIsPressedAsState()
    val active = dragged || pressed
    val motion =
        spring<androidx.compose.ui.unit.Dp>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    val trackHeight by animateDpAsState(if (active) 24.dp else 16.dp, motion, label = "touch track")
    val thumbWidth by animateDpAsState(if (active) 8.dp else 4.dp, motion, label = "touch thumb")
    val haptics = rememberPartsHaptics()
    Card(
        Modifier.fillMaxWidth(),
        shape = LocalPreferenceShape.current,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        draft.roundToInt().toString(),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Slider(
                modifier =
                    Modifier.semantics {
                        contentDescription = title
                        stateDescription = draft.roundToInt().toString()
                    },
                value = draft,
                onValueChange = {
                    val level = it.roundToInt().coerceIn(0, max)
                    if (level != draft.roundToInt()) haptics.detent()
                    draft = level.toFloat()
                },
                interactionSource = interactions,
                thumb = {
                    Box(
                        Modifier.size(thumbWidth, 44.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                },
                track = { state -> SliderDefaults.Track(state, Modifier.height(trackHeight)) },
                valueRange = 0f..max.toFloat(),
                steps = maxOf(0, max - 1),
                onValueChangeFinished = { change(draft.roundToInt()) },
            )
        }
    }
}

@Composable
fun TouchScreen(packageName: String, appName: String, back: () -> Unit) {
    val context = LocalContext.current
    val controls =
        remember(context, packageName) { TouchControls(context, packageName).also { it.reload() } }
    var revision by remember { mutableIntStateOf(0) }
    var help by rememberSaveable(packageName) { mutableStateOf(false) }
    var reset by rememberSaveable(packageName) { mutableStateOf(false) }
    var fineExpanded by rememberSaveable(packageName) { mutableStateOf(false) }
    OnResume {
        controls.reload()
        revision++
    }
    // Observe mutations through revision; the controller keeps the historical preference encoding.
    val state =
        remember(revision) {
            Triple(controls.available, controls.effective, controls.globalOverride)
        }
    val active = state.second
    PartsPage(
        stringResource(R.string.touch_controls_screen_title),
        back,
        actions = {
            IconButton(onClick = { help = true }) {
                Icon(PartsIcons.Info, stringResource(R.string.touch_controls_help_title))
            }
        },
    ) {
        ToggleCard(
            stringResource(R.string.touch_game_mode_title),
            appName,
            controls.enabled,
            state.first && packageName.isNotEmpty(),
            main = true,
        ) {
            controls.setValue(0, if (it) 1 else 0)
            revision++
        }
        val title =
            when {
                !state.first -> R.string.touch_controls_unavailable_title
                state.third -> R.string.touch_controls_global_title
                active -> R.string.touch_controls_active_title
                else -> R.string.touch_controls_start_title
            }
        val summary =
            when {
                !state.first -> R.string.touch_controls_unavailable_summary
                state.third -> R.string.touch_controls_global_summary
                active -> R.string.touch_controls_active_summary
                else -> R.string.touch_controls_start_summary
            }
        InfoCard(stringResource(title), stringResource(summary))
        if (state.third)
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(context, TouchSamplingSettingsActivity::class.java)
                    )
                }
            ) {
                Text(stringResource(R.string.touch_controls_global_title))
            }
        if (active) {
            if (controls.alioth) {
                SectionTitle(stringResource(R.string.touch_controls_mode_group))
                ChoiceCard(
                    stringResource(R.string.touch_controls_mode_title),
                    controls.preset.toString(),
                    ResourceChoices(
                            R.array.touch_controls_mode_entries,
                            R.array.touch_controls_mode_values,
                        )
                        .take(controls.presetMax + 1),
                    choiceIcon = { id, selected ->
                        ChoiceIcon(
                            when (id) {
                                "1" -> DialogSymbols.touch_app
                                "2" -> DialogSymbols.touch_app
                                "3" -> DialogSymbols.touch_app
                                else -> DialogSymbols.tune
                            },
                            selected,
                        )
                    },
                ) {
                    controls.setExtra("touch_expert", it.toInt())
                    revision++
                }
                Text(
                    stringResource(R.string.touch_controls_mode_help),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (controls.manual) {
                SectionTitle(stringResource(R.string.touch_controls_manual_group))
                PreferenceRow(0, 2) {
                    LevelCard(
                        stringResource(R.string.touch_response_title),
                        stringResource(R.string.touch_controls_response_summary),
                        controls.values[1],
                        controls.limits[1],
                    ) {
                        controls.setValue(1, it)
                        revision++
                    }
                }
                PreferenceRow(1, 2) {
                    LevelCard(
                        stringResource(R.string.touch_sensitivity_title),
                        stringResource(R.string.touch_controls_sensitivity_summary),
                        controls.values[2],
                        controls.limits[2],
                    ) {
                        controls.setValue(2, it)
                        revision++
                    }
                }
                if (controls.alioth) {
                    val rotation by
                        androidx.compose.animation.core.animateFloatAsState(
                            if (fineExpanded) 90f else 0f,
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            label = "fine adjustments",
                        )
                    TextButton(
                        onClick = { fineExpanded = !fineExpanded },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                stringResource(R.string.touch_controls_advanced_group),
                                Modifier.weight(1f),
                            )
                            Icon(
                                PartsIcons.Chevron,
                                null,
                                Modifier.graphicsLayer { rotationZ = rotation },
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = fineExpanded,
                        enter =
                            expandVertically(
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                            ),
                        exit =
                            shrinkVertically(
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                )
                            ),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            PreferenceRow(0, 2) {
                                LevelCard(
                                    stringResource(R.string.alioth_touch_aim),
                                    stringResource(R.string.touch_controls_aim_summary),
                                    controls.aim,
                                    controls.limits[4],
                                ) {
                                    controls.setExtra("touch_aim", it)
                                    revision++
                                }
                            }
                            PreferenceRow(1, 2) {
                                LevelCard(
                                    stringResource(R.string.alioth_touch_stability),
                                    stringResource(R.string.touch_controls_stability_summary),
                                    controls.stability,
                                    controls.limits[5],
                                ) {
                                    controls.setExtra("touch_stability", it)
                                    revision++
                                }
                            }
                        }
                    }
                }
            }
            SectionTitle(stringResource(R.string.touch_controls_edge_group))
            LevelCard(
                stringResource(R.string.touch_resistant_title),
                stringResource(R.string.touch_controls_edge_summary),
                controls.values[3],
                controls.limits[3],
            ) {
                controls.setValue(3, it)
                revision++
            }
            OutlinedButton(onClick = { reset = true }) {
                Text(stringResource(R.string.touch_controls_reset_title))
            }
        }
    }
    if (help || reset)
        AlertDialog(
            onDismissRequest = {
                help = false
                reset = false
            },
            title = {
                Text(
                    stringResource(
                        if (reset) R.string.touch_controls_reset_title
                        else R.string.touch_controls_help_title
                    ),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    stringResource(
                        if (reset) R.string.touch_controls_reset_summary
                        else R.string.touch_controls_help
                    ),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reset) {
                            controls.reset()
                            revision++
                        }
                        help = false
                        reset = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                if (reset)
                    TextButton(onClick = { reset = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
            },
        )
}
