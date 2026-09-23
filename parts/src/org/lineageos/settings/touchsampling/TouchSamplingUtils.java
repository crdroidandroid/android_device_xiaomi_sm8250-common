/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.touchsampling;

import android.content.Context;
import android.content.SharedPreferences;

import org.lineageos.settings.utils.FileUtils;

public final class TouchSamplingUtils {
    public static final String HTSR_FILE =
            "/sys/devices/virtual/touch/touch_dev/bump_sample_rate";
    private static final String PREF_FILE = "SHAREDHTSR";
    private static final String PREF_KEY = "SHAREDHTSR";

    private TouchSamplingUtils() {}

    public static boolean isSupported() {
        return FileUtils.isFileWritable(HTSR_FILE);
    }

    public static boolean isEnabled(Context context) {
        if (isSupported()) {
            String value = FileUtils.readOneLine(HTSR_FILE);
            if ("1".equals(value)) return true;
            if ("0".equals(value)) return false;
        }
        return context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .getInt(PREF_KEY, 0) == 1;
    }

    public static boolean setEnabled(Context context, boolean enabled) {
        if (!isSupported()) return false;
        if (!FileUtils.writeLine(HTSR_FILE, enabled ? "1" : "0")) return false;
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit()
                .putInt(PREF_KEY, enabled ? 1 : 0).apply();
        return true;
    }

    public static void restoreSamplingValue(Context context) {
        if (!isSupported()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        FileUtils.writeLine(HTSR_FILE, prefs.getInt(PREF_KEY, 0) == 1 ? "1" : "0");
    }
}
