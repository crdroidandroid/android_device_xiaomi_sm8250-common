/*
 * Copyright (C) 2015-2016 The CyanogenMod Project
 *               2017 The LineageOS Project
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

package org.lineageos.settings.display

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.compose.*

class DisplaySettingsActivity : PartsActivity() {
    @Composable
    override fun Content() {
        var revision by remember { mutableIntStateOf(0) }
        OnResume { revision++ }
        val dc =
            remember(revision) {
                DisplayUtils.isDcDimmingSupported() to DisplayUtils.isDcDimmingEnabled()
            }
        val hbm =
            remember(revision) { DisplayUtils.isHbmSupported() to DisplayUtils.isHbmEnabled() }
        fun apply(ok: Boolean) {
            if (!ok) Toast.makeText(this, R.string.parts_apply_failed, Toast.LENGTH_SHORT).show()
            revision++
        }
        PartsPage(stringResource(R.string.display_settings_title), { finish() }) {
            PreferenceRow(0, 2) {
                ToggleCard(
                    stringResource(R.string.dc_dimming_enable_title),
                    stringResource(
                        if (dc.first) R.string.dc_dimming_enable_summary
                        else R.string.dc_dimming_enable_summary_not_supported
                    ),
                    dc.first && dc.second,
                    dc.first,
                ) {
                    apply(DisplayUtils.setDcDimming(this@DisplaySettingsActivity, it))
                }
            }
            PreferenceRow(1, 2) {
                ToggleCard(
                    stringResource(R.string.hbm_mode_title),
                    stringResource(
                        if (hbm.first) R.string.hbm_mode_summary
                        else R.string.hbm_enable_summary_not_supported
                    ),
                    hbm.first && hbm.second,
                    hbm.first,
                ) {
                    apply(DisplayUtils.setHbm(this@DisplaySettingsActivity, it))
                }
            }
        }
    }
}
