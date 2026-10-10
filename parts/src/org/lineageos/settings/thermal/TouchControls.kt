/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.thermal

import android.content.Context
import android.util.Log
import org.lineageos.settings.R
import org.lineageos.settings.touchsampling.TouchSamplingUtils
import org.lineageos.settings.utils.PartsPreferences
import vendor.xiaomi.hardware.touchfeature.V1_0.ITouchFeature

/** Existing preferences remain the storage contract across the Java/Kotlin migration. */
class TouchControls(private val context: Context, val packageName: String) {
    private val prefs = PartsPreferences.getDefaultSharedPreferences(context)
    val alioth = AliothTouchProfile.isSupported()
    var available = false
        private set

    var defaultEdge = 0
        private set

    var presetMax = 3
        private set

    private val legacyMax = context.resources.getInteger(R.integer.smoothness_max)
    val limits = intArrayOf(1, legacyMax, legacyMax, legacyMax, 5, 5)
    var values = IntArray(4)
        private set

    var aim = 0
        private set

    var stability = 0
        private set

    var preset = 0
        private set

    val globalOverride
        get() = alioth && TouchSamplingUtils.isEnabled(context)

    val enabled
        get() = values[0] == 1

    val effective
        get() = packageName.isNotEmpty() && available && enabled && !globalOverride

    val manual
        get() = !alioth || preset == 0

    fun reload() {
        available = true
        if (alioth)
            try {
                val hal = checkNotNull(ITouchFeature.getService())
                fun range(mode: Int, expected: Int, maximum: Int): Int {
                    val max = hal.getTouchModeMaxValue(mode)
                    check(hal.getTouchModeMinValue(mode) == expected && max in expected..maximum)
                    return max
                }
                limits[1] = range(2, 1, 5)
                limits[2] = range(3, 1, 5)
                limits[3] = range(7, 0, 3)
                limits[4] = range(4, 1, 5)
                limits[5] = range(5, 1, 5)
                presetMax = range(6, 1, 3)
                defaultEdge = hal.getTouchModeDefValue(7)
                check(defaultEdge in 0..limits[3])
            } catch (error: Exception) {
                available = false
                Log.w("TouchControls", "Cannot query control ranges", error)
            }
        val saved =
            try {
                prefs.getString(packageName, null)?.split(',')
            } catch (_: ClassCastException) {
                null
            }
        values =
            IntArray(4) { index ->
                val fallback = if (index == 3) defaultEdge else 0
                (if (saved?.size == 4) saved[index].toIntOrNull() ?: fallback else fallback)
                    .coerceIn(0, limits[index])
            }
        aim = AliothTouchProfile.read(prefs, packageName, "touch_aim", limits[4])
        stability = AliothTouchProfile.read(prefs, packageName, "touch_stability", limits[5])
        preset = AliothTouchProfile.read(prefs, packageName, "touch_expert", presetMax)
    }

    private fun persist() {
        prefs.edit().putString(packageName, values.joinToString(",")).apply()
        ThermalUtils.startService(context)
    }

    fun setValue(index: Int, value: Int): Boolean {
        if (!available || packageName.isEmpty() || index !in 0..3 || value !in 0..limits[index])
            return false
        values[index] = value
        persist()
        return true
    }

    fun setExtra(key: String, value: Int): Boolean {
        val max =
            when (key) {
                "touch_aim" -> limits[4]
                "touch_stability" -> limits[5]
                "touch_expert" -> presetMax
                else -> return false
            }
        if (!alioth || !available || packageName.isEmpty() || value !in 0..max) return false
        prefs.edit().putInt(AliothTouchProfile.key(packageName, key), value).apply()
        when (key) {
            "touch_aim" -> aim = value
            "touch_stability" -> stability = value
            "touch_expert" -> preset = value
        }
        ThermalUtils.startService(context)
        return true
    }

    fun reset() {
        if (!available || packageName.isEmpty()) return
        values[1] = 0
        values[2] = 0
        values[3] = defaultEdge
        prefs
            .edit()
            .remove(AliothTouchProfile.key(packageName, "touch_aim"))
            .remove(AliothTouchProfile.key(packageName, "touch_stability"))
            .remove(AliothTouchProfile.key(packageName, "touch_expert"))
            .apply()
        aim = 0
        stability = 0
        preset = 0
        persist()
    }
}
