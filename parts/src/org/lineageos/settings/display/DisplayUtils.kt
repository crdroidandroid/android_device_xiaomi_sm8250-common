/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.display

import android.content.Context
import android.provider.Settings
import android.util.Log
import org.lineageos.settings.utils.FileUtils
import org.lineageos.settings.utils.PartsPreferences

object DisplayUtils {
    private const val PREVIOUS = "hbm_previous_brightness"

    @JvmStatic
    fun isDcDimmingSupported() = FileUtils.isFileWritable(DisplayNodes.getDcDimmingNode())

    @JvmStatic
    fun isHbmSupported() =
        FileUtils.isFileWritable(DisplayNodes.getHbmNode()) &&
            FileUtils.isFileWritable(DisplayNodes.getBacklight()) &&
            FileUtils.isFileReadable(DisplayNodes.getBacklightMax())

    @JvmStatic
    fun isDcDimmingEnabled() = FileUtils.readOneLine(DisplayNodes.getDcDimmingNode()) == "1"

    @JvmStatic fun isHbmEnabled() = FileUtils.readOneLine(DisplayNodes.getHbmNode()) == "1"

    @JvmStatic
    fun setDcDimming(context: Context, enabled: Boolean): Boolean {
        if (
            !isDcDimmingSupported() ||
                !FileUtils.writeLine(DisplayNodes.getDcDimmingNode(), if (enabled) "1" else "0")
        )
            return false
        PartsPreferences.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(DisplayNodes.getDcDimmingEnableKey(), enabled)
            .apply()
        return true
    }

    @JvmStatic
    fun setHbm(context: Context, enabled: Boolean): Boolean {
        if (!isHbmSupported()) return false
        val prefs = PartsPreferences.getDefaultSharedPreferences(context)
        if (enabled) {
            val hadPrevious = prefs.contains(PREVIOUS)
            if (!isHbmEnabled() && !hadPrevious)
                prefs
                    .edit()
                    .putInt(
                        PREVIOUS,
                        Settings.System.getInt(
                            context.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            255,
                        ),
                    )
                    .apply()
            fun fail(rollback: Boolean): Boolean {
                if (rollback) FileUtils.writeLine(DisplayNodes.getHbmNode(), "0")
                if (!hadPrevious) prefs.edit().remove(PREVIOUS).apply()
                return false
            }
            val max = FileUtils.readOneLine(DisplayNodes.getBacklightMax())?.trim()
            if (max.isNullOrEmpty()) return fail(false)
            if (!FileUtils.writeLine(DisplayNodes.getHbmNode(), "1")) return fail(false)
            if (!FileUtils.writeLine(DisplayNodes.getBacklight(), max)) return fail(true)
            if (
                !Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    255,
                )
            )
                return fail(true)
        } else {
            if (!FileUtils.writeLine(DisplayNodes.getHbmNode(), "0")) return false
            if (prefs.contains(PREVIOUS)) {
                if (
                    !Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        prefs.getInt(PREVIOUS, 255),
                    )
                ) {
                    prefs.edit().putBoolean(DisplayNodes.getHbmEnableKey(), false).apply()
                    return false
                }
                prefs.edit().remove(PREVIOUS).apply()
            }
        }
        prefs.edit().putBoolean(DisplayNodes.getHbmEnableKey(), enabled).apply()
        return true
    }

    @JvmStatic
    fun restore(context: Context) {
        val prefs = PartsPreferences.getDefaultSharedPreferences(context)
        if (
            isDcDimmingSupported() &&
                !FileUtils.writeLine(
                    DisplayNodes.getDcDimmingNode(),
                    if (prefs.getBoolean(DisplayNodes.getDcDimmingEnableKey(), false)) "1" else "0",
                )
        )
            Log.w("DisplayUtils", "Cannot restore DC dimming")
        if (
            isHbmSupported() &&
                !setHbm(context, prefs.getBoolean(DisplayNodes.getHbmEnableKey(), false))
        )
            Log.w("DisplayUtils", "Cannot restore HBM or previous brightness")
    }
}
