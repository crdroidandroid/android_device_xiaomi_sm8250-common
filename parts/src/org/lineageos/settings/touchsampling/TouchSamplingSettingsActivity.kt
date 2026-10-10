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

package org.lineageos.settings.touchsampling

import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.compose.*
import org.lineageos.settings.thermal.ThermalUtils

class TouchSamplingSettingsActivity : PartsActivity() {
    @Composable
    override fun Content() {
        var revision by remember { mutableIntStateOf(0) }
        OnResume { revision++ }
        val supported = remember(revision) { TouchSamplingUtils.isSupported() }
        val enabled = remember(revision) { supported && TouchSamplingUtils.isEnabled(this) }
        PartsPage(stringResource(R.string.htsr_title), { finish() }) {
            ToggleCard(
                stringResource(R.string.htsr_enable_title),
                stringResource(R.string.htsr_enable_summary),
                enabled,
                supported,
                main = true,
            ) {
                if (TouchSamplingUtils.setEnabled(this@TouchSamplingSettingsActivity, it))
                    ThermalUtils.startService(this@TouchSamplingSettingsActivity)
                else
                    Toast.makeText(
                            this@TouchSamplingSettingsActivity,
                            R.string.parts_apply_failed,
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                revision++
            }
        }
    }
}
