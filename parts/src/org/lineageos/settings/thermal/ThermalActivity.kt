/*
 * Copyright (C) 2020-2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.settings.R
import org.lineageos.settings.compose.*

class ThermalActivity : PartsActivity() {
    @Composable
    override fun Content() {
        val utils = remember { ThermalUtils(this) }
        var revision by remember { mutableIntStateOf(0) }
        var selectedPackage by rememberSaveable { mutableStateOf("") }
        var selectedLabel by rememberSaveable { mutableStateOf("") }
        val uiPrefs = remember { getSharedPreferences("thermal_ui", Context.MODE_PRIVATE) }
        var expanded by remember {
            mutableStateOf(uiPrefs.getBoolean("system_controls_expanded", false))
        }
        OnResume { revision++ }
        BackHandler(selectedPackage.isNotEmpty()) { selectedPackage = "" }
        if (selectedPackage.isNotEmpty()) {
            TouchScreen(selectedPackage, selectedLabel) { selectedPackage = "" }
            return
        }
        val region = remember(revision) { ThermalUtils.getSelectedRegion() }
        val profiles = ThermalProfiles.getProfiles(region)
        val choices =
            listOf("0" to stringResource(R.string.thermal_system_default)) +
                profiles.map { it.storageState.toString() to stringResource(it.titleRes) }
        val descriptions =
            mapOf("0" to stringResource(R.string.thermal_system_default_summary)) +
                profiles.associate { it.storageState.toString() to stringResource(it.summaryRes) }
        AppListPage(
            stringResource(R.string.thermal_title),
            { finish() },
            actions = {
                IconButton(
                    onClick = {
                        startActivity(Intent(this@ThermalActivity, ThermalInfoActivity::class.java))
                    }
                ) {
                    Icon(PartsIcons.Info, stringResource(R.string.thermal_info_title))
                }
            },
            header = {
                Column(
                    modifier =
                        Modifier.animateContentSize(
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            )
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val expansionRotation by
                        animateFloatAsState(
                            if (expanded) 90f else 0f,
                            spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                            label = "system profile",
                        )
                    val base = ThermalProfiles.findBySconfig(region, utils.getBaseSconfig())
                    Card(
                        onClick = {
                            expanded = !expanded
                            uiPrefs.edit().putBoolean("system_controls_expanded", expanded).apply()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                topStart = 24.dp,
                                topEnd = 24.dp,
                                bottomStart = if (expanded) 4.dp else 24.dp,
                                bottomEnd = if (expanded) 4.dp else 24.dp,
                            ),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright
                            ),
                    ) {
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = if (expanded) 48.dp else 72.dp)
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = if (expanded) 8.dp else 16.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    stringResource(R.string.thermal_system_profile_title),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (!expanded) {
                                    Text(
                                        stringResource(
                                            R.string.thermal_system_compact_summary,
                                            stringResource(
                                                base?.titleRes ?: R.string.thermal_normal
                                            ),
                                            stringResource(
                                                if (region == 1) R.string.thermal_region_india
                                                else R.string.thermal_region_global
                                            ),
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Icon(
                                PartsIcons.Chevron,
                                stringResource(
                                    if (expanded) R.string.thermal_system_collapse
                                    else R.string.thermal_system_expand
                                ),
                                Modifier.rotate(expansionRotation),
                            )
                        }
                    }
                    if (expanded) {
                        val regions =
                            listOf("0" to stringResource(R.string.thermal_region_global)) +
                                if (ThermalUtils.isIndiaMapAvailable())
                                    listOf("1" to stringResource(R.string.thermal_region_india))
                                else emptyList()
                        PreferenceRow(1, 3) {
                            ChoiceCard(
                                stringResource(R.string.thermal_profile_set_title),
                                region.toString(),
                                regions,
                                choiceIcon = { id, selected ->
                                    ChoiceIcon(
                                        if (id == "0") DialogSymbols.public
                                        else DialogSymbols.location_on,
                                        selected,
                                    )
                                },
                            ) {
                                if (!utils.setSelectedRegion(it.toInt()))
                                    Toast.makeText(
                                            this@ThermalActivity,
                                            R.string.parts_apply_failed,
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                revision++
                            }
                        }
                        PreferenceRow(2, 3) {
                            ChoiceCard(
                                stringResource(R.string.thermal_profile_dialog_title),
                                utils.getBaseSconfig().toString(),
                                profiles.map {
                                    it.sconfig.toString() to stringResource(it.titleRes)
                                },
                                descriptions =
                                    profiles.associate {
                                        it.sconfig.toString() to stringResource(it.summaryRes)
                                    },
                                choiceIcon = { id, selected ->
                                    profiles
                                        .firstOrNull { it.sconfig.toString() == id }
                                        ?.let {
                                            ThermalProfileIcon(
                                                it.iconRes,
                                                compact = true,
                                                selected = selected,
                                            )
                                        }
                                },
                            ) {
                                if (!utils.setBaseSconfig(it.toInt()))
                                    Toast.makeText(
                                            this@ThermalActivity,
                                            R.string.parts_apply_failed,
                                            Toast.LENGTH_SHORT,
                                        )
                                        .show()
                                revision++
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            },
        ) { app ->
            val state =
                remember(app.packageName, revision) { utils.getStateForPackage(app.packageName) }
            AppChoiceCard(
                app,
                state.toString(),
                choices,
                descriptions = descriptions,
                choiceIcon = { id, selected ->
                    val chosen = profiles.firstOrNull { it.storageState.toString() == id }
                    val inherited = profiles.firstOrNull { it.sconfig == utils.getBaseSconfig() }
                    ThermalProfileIcon(
                        (chosen ?: inherited)?.iconRes ?: R.drawable.ic_thermal_default,
                        compact = true,
                        selected = selected,
                    )
                },
                trailing = {
                    Icon(
                        PartsIcons.Chevron,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    VerticalDivider(Modifier.height(40.dp))
                    IconButton(
                        onClick = {
                            selectedPackage = app.packageName
                            selectedLabel = app.label
                        }
                    ) {
                        Icon(
                            PartsIcons.Tune,
                            stringResource(R.string.touch_controls_screen_title) + " · " + app.label,
                        )
                    }
                },
            ) {
                utils.writePackage(app.packageName, it.toInt())
                revision++
            }
        }
    }
}
