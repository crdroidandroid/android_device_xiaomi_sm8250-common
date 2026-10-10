/*
 * Copyright (C) 2021 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings

import android.hardware.display.DisplayManager
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.Display
import android.widget.Toast
import java.util.Locale
import kotlin.math.abs
import org.lineageos.settings.refreshrate.RefreshUtils

class RefreshRateTileService : TileService() {
    private var rates = emptyList<Float>()

    override fun onCreate() {
        super.onCreate()
        loadRates()
    }

    private fun loadRates() {
        val display =
            getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
                ?: return
        val current = display.mode
        rates =
            display.supportedModes
                .filter {
                    it.physicalWidth == current.physicalWidth &&
                        it.physicalHeight == current.physicalHeight
                }
                .map { String.format(Locale.US, "%.02f", it.refreshRate).toFloat() }
                .distinct()
                .sorted()
    }

    private fun nearest(value: Float) = rates.indices.minByOrNull { abs(rates[it] - value) } ?: -1

    private fun read(key: String, fallback: Float) =
        Settings.System.getFloat(contentResolver, key, fallback)

    private fun write(rate: Float): Boolean {
        val oldMin = read(Settings.System.MIN_REFRESH_RATE, rate)
        val oldPeak = read(Settings.System.PEAK_REFRESH_RATE, rate)
        if (!Settings.System.putFloat(contentResolver, Settings.System.MIN_REFRESH_RATE, rate))
            return false
        if (!Settings.System.putFloat(contentResolver, Settings.System.PEAK_REFRESH_RATE, rate)) {
            Settings.System.putFloat(contentResolver, Settings.System.MIN_REFRESH_RATE, oldMin)
            Settings.System.putFloat(contentResolver, Settings.System.PEAK_REFRESH_RATE, oldPeak)
            return false
        }
        return true
    }

    private fun format(rate: Float) =
        String.format(Locale.US, "%.02f Hz", rate).replace(Regex("[.,]00"), "")

    private fun update() {
        val tile = qsTile ?: return
        if (rates.isEmpty()) {
            tile.state = Tile.STATE_UNAVAILABLE
            tile.updateTile()
            return
        }
        val min = rates[nearest(read(Settings.System.MIN_REFRESH_RATE, rates.first()))]
        val max = rates[nearest(read(Settings.System.PEAK_REFRESH_RATE, rates.last()))]
        val label = if (min == max) format(min) else "${format(min)} - ${format(max)}"
        tile.contentDescription = label
        tile.subtitle = label
        tile.state = if (min == max) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        if (rates.isEmpty()) loadRates()
        update()
    }

    override fun onClick() {
        super.onClick()
        if (rates.isEmpty()) {
            update()
            return
        }
        val next =
            rates[(nearest(read(Settings.System.MIN_REFRESH_RATE, rates.first())) + 1) % rates.size]
        if (write(next)) RefreshUtils.updateUserBaseline(this, next, next)
        else Toast.makeText(this, R.string.parts_apply_failed, Toast.LENGTH_SHORT).show()
        update()
    }
}
