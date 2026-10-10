/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.touchsampling

import android.content.Context
import org.lineageos.settings.utils.FileUtils

object TouchSamplingUtils {
    const val HTSR_FILE = "/sys/devices/virtual/touch/touch_dev/bump_sample_rate"
    private const val PREF = "SHAREDHTSR"

    @JvmStatic fun isSupported() = FileUtils.isFileWritable(HTSR_FILE)

    @JvmStatic
    fun isEnabled(context: Context): Boolean {
        if (isSupported())
            when (FileUtils.readOneLine(HTSR_FILE)) {
                "1" -> return true
                "0" -> return false
            }
        return context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(PREF, 0) == 1
    }

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        if (!isSupported() || !FileUtils.writeLine(HTSR_FILE, if (enabled) "1" else "0"))
            return false
        context
            .getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putInt(PREF, if (enabled) 1 else 0)
            .apply()
        return true
    }

    @JvmStatic
    fun restoreSamplingValue(context: Context) {
        if (isSupported())
            FileUtils.writeLine(
                HTSR_FILE,
                if (context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(PREF, 0) == 1)
                    "1"
                else "0",
            )
    }
}
