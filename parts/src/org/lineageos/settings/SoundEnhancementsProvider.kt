/*
 * Copyright (C) 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings

import com.android.settingslib.drawer.EntriesProvider
import com.android.settingslib.drawer.EntryController

/** Supplies the heading for device-provided sound controls in Settings. */
class SoundEnhancementsProvider : EntriesProvider() {
    override fun createEntryControllers(): List<EntryController> = listOf(
        object : EntryController() {
            override fun getKey() = "xiaomi_audio_enhancements"

            override fun getMetaData(): MetaData =
                MetaData("com.android.settings.category.ia.sound")
                    .setTitle(R.string.sound_enhancements_title)
        }
    )
}
