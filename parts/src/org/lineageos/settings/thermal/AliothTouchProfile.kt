/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.thermal

import android.content.SharedPreferences
import android.os.Build
import vendor.xiaomi.hardware.touchfeature.V1_0.ITouchFeature

object AliothTouchProfile {
    @JvmStatic fun isSupported() = Build.DEVICE == "alioth" || Build.DEVICE == "aliothin"

    @JvmStatic fun key(pkg: String, setting: String) = "alioth_touch:$pkg:$setting"

    @JvmStatic
    fun read(prefs: SharedPreferences, pkg: String, setting: String, max: Int): Int =
        try {
            prefs.getInt(key(pkg, setting), 0).coerceIn(0, max)
        } catch (_: ClassCastException) {
            0
        }

    @JvmStatic
    fun apply(hal: ITouchFeature, prefs: SharedPreferences, pkg: String) {
        if (!isSupported()) return
        applyMode(hal, 4, read(prefs, pkg, "touch_aim", 5))
        applyMode(hal, 5, read(prefs, pkg, "touch_stability", 5))
        val expert = read(prefs, pkg, "touch_expert", 3)
        if (expert > 0)
            applyMode(hal, 6, expert) // Presets last: manual writes disable expert mode.
    }

    @JvmStatic
    fun applyMode(hal: ITouchFeature, mode: Int, value: Int) {
        if (value == 0) hal.resetTouchMode(mode)
        else if (value in hal.getTouchModeMinValue(mode)..hal.getTouchModeMaxValue(mode))
            hal.setTouchMode(mode, value)
    }
}
