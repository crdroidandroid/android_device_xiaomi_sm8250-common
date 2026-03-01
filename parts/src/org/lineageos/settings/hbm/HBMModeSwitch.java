/*
* Copyright (C) 2016 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.lineageos.settings.hbm;
import android.provider.Settings;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceManager;
import android.widget.Toast;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class HBMModeSwitch implements OnPreferenceChangeListener {
    private static final String DC_DIMMING_KEY = "dc_dimming";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";
    private Context mContext;

    public HBMModeSwitch(Context context) {
        mContext = context;
    }

    public static String getHBM() {
        if (FileUtils.isFileWritable(HBM_NODE)) {
            return HBM_NODE;
        }
        return null;
    }

    public static String getBACKLIGHT() {
        if (FileUtils.isFileWritable(BACKLIGHT_NODE)) {
            return BACKLIGHT_NODE;
        }
        return null;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;

        SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean dcDimmingEnabled = prefs.getBoolean(DC_DIMMING_KEY, false);

        if (enabled && dcDimmingEnabled) {
            Toast.makeText(
                    mContext,
                    R.string.hbm_disable_dc_dimming_first,
                    Toast.LENGTH_SHORT
            ).show();
            return false;
        }

        if (enabled) {
            // Save current brightness
            int currentBrightness = Settings.System.getInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
            );
            prefs.edit().putInt("last_brightness", currentBrightness).apply();

            FileUtils.writeLine(getHBM(), "1");
            FileUtils.writeLine(getBACKLIGHT(), "2047");
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    255
            );
        } else {
            FileUtils.writeLine(getHBM(), "0");

            // Restore brightness
            int lastBrightness = prefs.getInt("last_brightness", 128);
            Settings.System.putInt(
                    mContext.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    lastBrightness
            );
        }
        return true;
    }
}
