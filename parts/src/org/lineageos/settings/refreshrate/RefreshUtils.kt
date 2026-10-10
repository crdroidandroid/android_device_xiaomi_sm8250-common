/*
 * Copyright (C) 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.refreshrate

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import org.lineageos.settings.utils.PartsPreferences

class RefreshUtils(context: Context) {
    companion object {
        const val STATE_DEFAULT = 0
        const val STATE_STANDARD = 1
        const val STATE_EXTREME = 2
        private const val ACTIVE = "refresh_override_active"
        private const val BASE_MIN = "refresh_base_min"
        private const val BASE_PEAK = "refresh_base_peak"
        private const val APPLIED_MIN = "refresh_applied_min"
        private const val APPLIED_PEAK = "refresh_applied_peak"
        private val PREFIXES = listOf("refresh.standard=", "refresh.extreme=")

        @JvmStatic
        fun startService(context: Context) {
            context.startService(Intent(context, RefreshService::class.java))
        }

        @JvmStatic
        fun updateUserBaseline(context: Context, min: Float, peak: Float) {
            val prefs = PartsPreferences.getDefaultSharedPreferences(context.applicationContext)
            if (prefs.getBoolean(ACTIVE, false))
                prefs.edit().putFloat(BASE_MIN, min).putFloat(BASE_PEAK, peak).apply()
        }
    }

    private val app = context.applicationContext
    private val prefs = PartsPreferences.getDefaultSharedPreferences(app)

    private fun writeValue(value: String) {
        prefs.edit().putString("refresh_control", value).apply()
    }

    private fun getValue(): List<String> {
        val value = prefs.getString("refresh_control", null)?.split(':')
        if (
            value != null &&
                value.size == PREFIXES.size &&
                value.indices.all { value[it].startsWith(PREFIXES[it]) }
        )
            return value
        return PREFIXES.also { writeValue(it.joinToString(":")) }
    }

    private fun hasPackage(profile: String, packageName: String): Boolean =
        packageName.isNotEmpty() &&
            '=' in profile &&
            packageName in profile.substringAfter('=').split(',')

    fun writePackage(packageName: String, mode: Int) {
        if (packageName.isEmpty()) return
        val modes =
            getValue()
                .map { profile ->
                    profile.substringBefore('=') +
                        "=" +
                        profile
                            .substringAfter('=')
                            .split(',')
                            .filter { it.isNotEmpty() && it != packageName }
                            .joinToString(",", postfix = "")
                            .let { if (it.isEmpty()) "" else "$it," }
                }
                .toMutableList()
        if (mode == STATE_STANDARD || mode == STATE_EXTREME) modes[mode - 1] += "$packageName,"
        writeValue(modes.joinToString(":"))
        startService(app)
    }

    fun getStateForPackage(packageName: String): Int {
        val modes = getValue()
        return when {
            hasPackage(modes[0], packageName) -> STATE_STANDARD
            hasPackage(modes[1], packageName) -> STATE_EXTREME
            else -> STATE_DEFAULT
        }
    }

    private fun currentMin() =
        Settings.System.getFloat(app.contentResolver, Settings.System.MIN_REFRESH_RATE, 0f)

    private fun currentPeak() =
        Settings.System.getFloat(app.contentResolver, Settings.System.PEAK_REFRESH_RATE, 120f)

    private fun writeRates(min: Float, peak: Float): Boolean {
        val oldMin = currentMin()
        if (!Settings.System.putFloat(app.contentResolver, Settings.System.MIN_REFRESH_RATE, min))
            return false
        if (
            !Settings.System.putFloat(app.contentResolver, Settings.System.PEAK_REFRESH_RATE, peak)
        ) {
            Settings.System.putFloat(app.contentResolver, Settings.System.MIN_REFRESH_RATE, oldMin)
            return false
        }
        return true
    }

    private fun captureBaseline() {
        if (!prefs.getBoolean(ACTIVE, false))
            prefs
                .edit()
                .putFloat(BASE_MIN, currentMin())
                .putFloat(BASE_PEAK, currentPeak())
                .putBoolean(ACTIVE, true)
                .apply()
    }

    private fun preserveExternalChanges() {
        if (!prefs.getBoolean(ACTIVE, false)) return
        val editor = prefs.edit()
        val min = currentMin()
        val peak = currentPeak()
        if (
            prefs.contains(APPLIED_MIN) &&
                java.lang.Float.compare(min, prefs.getFloat(APPLIED_MIN, min)) != 0
        )
            editor.putFloat(BASE_MIN, min)
        if (
            prefs.contains(APPLIED_PEAK) &&
                java.lang.Float.compare(peak, prefs.getFloat(APPLIED_PEAK, peak)) != 0
        )
            editor.putFloat(BASE_PEAK, peak)
        editor.apply()
    }

    fun restoreDefaultRates(): Boolean {
        if (!prefs.getBoolean(ACTIVE, false)) return true
        preserveExternalChanges()
        if (!writeRates(prefs.getFloat(BASE_MIN, 0f), prefs.getFloat(BASE_PEAK, 120f))) {
            Log.w("RefreshUtils", "Cannot restore refresh baseline")
            return false
        }
        prefs
            .edit()
            .remove(ACTIVE)
            .remove(BASE_MIN)
            .remove(BASE_PEAK)
            .remove(APPLIED_MIN)
            .remove(APPLIED_PEAK)
            .apply()
        return true
    }

    fun setRefreshRate(packageName: String): Boolean {
        val state = getStateForPackage(packageName)
        if (state == STATE_DEFAULT) return restoreDefaultRates()
        preserveExternalChanges()
        captureBaseline()
        val peak = if (state == STATE_STANDARD) 60f else 120f
        val min = minOf(prefs.getFloat(BASE_MIN, 0f), peak)
        if (!writeRates(min, peak)) {
            Log.w("RefreshUtils", "Cannot apply per-app refresh rate")
            restoreDefaultRates()
            return false
        }
        prefs.edit().putFloat(APPLIED_MIN, min).putFloat(APPLIED_PEAK, peak).apply()
        return true
    }
}
