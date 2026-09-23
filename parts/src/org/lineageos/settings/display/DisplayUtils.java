/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.display;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

public final class DisplayUtils {
    private static final String TAG = "DisplayUtils";
    private static final String PREF_HBM_PREVIOUS_BRIGHTNESS = "hbm_previous_brightness";

    private DisplayUtils() {}

    public static boolean isDcDimmingSupported() {
        return FileUtils.isFileWritable(DisplayNodes.getDcDimmingNode());
    }

    public static boolean isHbmSupported() {
        return FileUtils.isFileWritable(DisplayNodes.getHbmNode())
                && FileUtils.isFileWritable(DisplayNodes.getBacklight())
                && FileUtils.isFileReadable(DisplayNodes.getBacklightMax());
    }

    public static boolean isDcDimmingEnabled() {
        return "1".equals(FileUtils.readOneLine(DisplayNodes.getDcDimmingNode()));
    }

    public static boolean isHbmEnabled() {
        return "1".equals(FileUtils.readOneLine(DisplayNodes.getHbmNode()));
    }

    public static boolean setDcDimming(Context context, boolean enabled) {
        if (!isDcDimmingSupported()) return false;
        if (!FileUtils.writeLine(DisplayNodes.getDcDimmingNode(), enabled ? "1" : "0")) {
            return false;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(DisplayNodes.getDcDimmingEnableKey(), enabled).apply();
        return true;
    }

    public static boolean setHbm(Context context, boolean enabled) {
        if (!isHbmSupported()) return false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (enabled) {
            boolean hadPreviousBrightness = prefs.contains(PREF_HBM_PREVIOUS_BRIGHTNESS);
            if (!isHbmEnabled() && !hadPreviousBrightness) {
                int previousBrightness = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 255);
                prefs.edit().putInt(PREF_HBM_PREVIOUS_BRIGHTNESS, previousBrightness).apply();
            }
            String max = FileUtils.readOneLine(DisplayNodes.getBacklightMax());
            if (max == null || max.trim().isEmpty()) {
                if (!hadPreviousBrightness) prefs.edit().remove(PREF_HBM_PREVIOUS_BRIGHTNESS).apply();
                return false;
            }

            if (!FileUtils.writeLine(DisplayNodes.getHbmNode(), "1")) {
                if (!hadPreviousBrightness) prefs.edit().remove(PREF_HBM_PREVIOUS_BRIGHTNESS).apply();
                return false;
            }
            if (!FileUtils.writeLine(DisplayNodes.getBacklight(), max.trim())) {
                FileUtils.writeLine(DisplayNodes.getHbmNode(), "0");
                if (!hadPreviousBrightness) prefs.edit().remove(PREF_HBM_PREVIOUS_BRIGHTNESS).apply();
                return false;
            }
            if (!Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 255)) {
                FileUtils.writeLine(DisplayNodes.getHbmNode(), "0");
                if (!hadPreviousBrightness) prefs.edit().remove(PREF_HBM_PREVIOUS_BRIGHTNESS).apply();
                return false;
            }
        } else {
            if (!FileUtils.writeLine(DisplayNodes.getHbmNode(), "0")) return false;
            if (prefs.contains(PREF_HBM_PREVIOUS_BRIGHTNESS)) {
                int previous = prefs.getInt(PREF_HBM_PREVIOUS_BRIGHTNESS, 255);
                Settings.System.putInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, previous);
                prefs.edit().remove(PREF_HBM_PREVIOUS_BRIGHTNESS).apply();
            }
        }

        prefs.edit().putBoolean(DisplayNodes.getHbmEnableKey(), enabled).apply();
        return true;
    }

    public static void restore(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if (isDcDimmingSupported()) {
            boolean enabled = prefs.getBoolean(DisplayNodes.getDcDimmingEnableKey(), false);
            if (!FileUtils.writeLine(DisplayNodes.getDcDimmingNode(), enabled ? "1" : "0")) {
                Log.w(TAG, "Cannot restore DC dimming");
            }
        }

        if (isHbmSupported()) {
            boolean enabled = prefs.getBoolean(DisplayNodes.getHbmEnableKey(), false);
            if (enabled) {
                if (!setHbm(context, true)) Log.w(TAG, "Cannot restore HBM");
            } else if (!FileUtils.writeLine(DisplayNodes.getHbmNode(), "0")) {
                Log.w(TAG, "Cannot restore HBM off state");
            }
        }
    }
}
