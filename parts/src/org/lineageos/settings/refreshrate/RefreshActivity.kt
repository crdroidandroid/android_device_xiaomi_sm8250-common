/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.refreshrate

import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import org.lineageos.settings.R
import org.lineageos.settings.compose.*

class RefreshActivity : PartsActivity() {
    @Composable
    override fun Content() {
        val utils = remember { RefreshUtils(this) }
        var revision by remember { mutableIntStateOf(0) }
        OnResume { revision++ }
        val choices =
            listOf(
                "0" to stringResource(R.string.refresh_default),
                "1" to stringResource(R.string.refresh_standard),
                "2" to stringResource(R.string.refresh_extreme),
            )
        AppListPage(stringResource(R.string.refresh_title), { finish() }) { app ->
            val value =
                remember(app.packageName, revision) {
                    utils.getStateForPackage(app.packageName).toString()
                }
            AppChoiceCard(
                app,
                value,
                choices,
                choiceIcon = { id, selected ->
                    ChoiceIcon(
                        when (id) {
                            "1" -> RefreshIcons.standard
                            "2" -> DialogSymbols.speed
                            else -> DialogSymbols.settings
                        },
                        selected,
                    )
                },
            ) {
                utils.writePackage(app.packageName, it.toInt())
                revision++
            }
        }
    }
}
