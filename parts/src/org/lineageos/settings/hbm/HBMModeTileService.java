/*
* Copyright (C) 2018 The OmniROM Project
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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class HBMModeTileService extends TileService {

    private static final String DC_DIMMING_KEY = "dc_dimming";
    private static final String HBM_KEY = "hbm";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";

    private void updateUI(boolean enabled) {
        final Tile tile = getQsTile();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateUI(sharedPrefs.getBoolean(HBM_KEY, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);

        final boolean dcDimmingEnabled =
                sharedPrefs.getBoolean(DC_DIMMING_KEY, false);

        if (dcDimmingEnabled) {
            Toast.makeText(
                    this,
                    R.string.hbm_disable_dc_dimming_first,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final boolean enabled =
                !sharedPrefs.getBoolean(HBM_KEY, false);

        if (enabled) {
            // Save current brightness level
            int currentBrightness = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
            );
            sharedPrefs.edit()
                    .putInt("last_brightness", currentBrightness)
                    .apply();

            FileUtils.writeLine(HBM_NODE, "1");
            FileUtils.writeLine(BACKLIGHT_NODE, "2047");
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    255
            );
        } else {
            FileUtils.writeLine(HBM_NODE, "0");

            // Restore last brightness level
            int lastBrightness =
                    sharedPrefs.getInt("last_brightness", 128);
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    lastBrightness
            );
        }

        sharedPrefs.edit().putBoolean(HBM_KEY, enabled).apply();
        updateUI(enabled);
    }
}
