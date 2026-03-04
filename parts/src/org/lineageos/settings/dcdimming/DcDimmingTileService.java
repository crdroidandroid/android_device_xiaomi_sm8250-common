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
package org.lineageos.settings.dcdimming;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

import java.io.File;

public class DcDimmingTileService extends TileService {

    private static final String DC_DIMMING_KEY = "dc_dimming";
    private static final String DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/dimlayer_exposure";
    private static final String HBM_KEY = "hbm";

    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener =
            (prefs, key) -> {
                if (DC_DIMMING_KEY.equals(key)) {
                    updateUI(prefs.getBoolean(DC_DIMMING_KEY, false));
                }
            };

    private void updateUI(boolean enabled) {
        final Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateUI(sharedPrefs.getBoolean(DC_DIMMING_KEY, false));
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

        boolean hbmEnabled = sharedPrefs.getBoolean(HBM_KEY, false);

        if (hbmEnabled) {
            Toast.makeText(
                    this,
                    R.string.dc_dimming_disable_hbm_first,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final boolean enabled = !sharedPrefs.getBoolean(DC_DIMMING_KEY, false);

        FileUtils.writeLine(DC_DIMMING_NODE, enabled ? "1" : "0");
        // putBoolean sẽ trigger mPrefsListener -> updateUI tự động
        sharedPrefs.edit().putBoolean(DC_DIMMING_KEY, enabled).apply();
    }
}