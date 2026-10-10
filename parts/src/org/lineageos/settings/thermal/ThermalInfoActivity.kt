/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.thermal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.lineageos.settings.R
import org.lineageos.settings.compose.*

class ThermalInfoActivity : PartsActivity() {
    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableIntStateOf(-1) }
        var revision by remember { mutableIntStateOf(0) }
        OnResume { revision++ }
        val region = remember(revision) { ThermalUtils.getSelectedRegion() }
        val profiles = ThermalProfiles.getProfiles(region)
        val profile = profiles.firstOrNull { it.storageState == selected }
        BackHandler(profile != null) { selected = -1 }
        if (profile != null) {
            PartsPage(stringResource(profile.titleRes), { selected = -1 }) {
                InfoCard(stringResource(profile.titleRes), stringResource(profile.summaryRes))
                Text(
                    stringResource(
                        R.string.thermal_detail_meta,
                        stringResource(
                            if (region == 1) R.string.thermal_region_india
                            else R.string.thermal_region_global
                        ),
                        profile.sconfig,
                        profile.policy.configName,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                val details =
                    remember(profile, resources.configuration) {
                        ThermalDetails(this@ThermalInfoActivity)
                    }
                Text(
                    stringResource(R.string.thermal_detail_clock_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                SectionTitle(stringResource(R.string.thermal_detail_cpu_title))
                details
                    .cpu(profile.policy)
                    .split("\n\n")
                    .filter { it.isNotBlank() }
                    .forEach { cluster ->
                        InfoCard(cluster.substringBefore('\n'), cluster.substringAfter('\n', ""))
                        Spacer(Modifier.height(8.dp))
                    }
                InfoCard(
                    stringResource(R.string.thermal_detail_gpu_title),
                    details.gpu(profile.policy),
                )
                InfoCard(
                    stringResource(R.string.thermal_detail_controls_title),
                    details.controls(profile.policy),
                )
                InfoCard(
                    stringResource(R.string.thermal_detail_sensor_title),
                    details.sensor(profile.policy),
                )
            }
        } else
            PartsPage(stringResource(R.string.thermal_info_title), { finish() }) {
                SectionTitle(
                    stringResource(
                        R.string.thermal_info_header_title,
                        stringResource(
                            if (region == 1) R.string.thermal_region_india
                            else R.string.thermal_region_global
                        ),
                    )
                )
                Text(
                    stringResource(R.string.thermal_info_header, profiles.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(12.dp))
                profiles.forEachIndexed { index, item ->
                    PreferenceRow(index, profiles.size) {
                        Card(
                            onClick = { selected = item.storageState },
                            modifier = Modifier.fillMaxWidth(),
                            shape = LocalPreferenceShape.current,
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright
                                ),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                ThermalProfileIcon(item.iconRes)
                                Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        stringResource(item.titleRes),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        stringResource(item.summaryRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    PartsIcons.Chevron,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
    }
}
