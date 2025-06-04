/*
 * Copyright (C) 2021 crDroid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.refreshrate.tile;

import android.content.Context;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RefreshRateTileService extends TileService {
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private static final String KEY_PREFERRED_REFRESH_RATE = "preferred_refresh_rate";
    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";

    private Context context;
    private Tile tile;

    private final List<Float> availableRates = new ArrayList<>();
    private int activeRateMin;
    private int activeRateMax;
    private boolean isAdaptiveRateActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        Display.Mode mode = context.getDisplay().getMode();
        Display.Mode[] modes = context.getDisplay().getSupportedModes();
        for (Display.Mode m : modes) {
            float rate = Float.valueOf(String.format(Locale.US, "%.02f", m.getRefreshRate()));
            if (m.getPhysicalWidth() == mode.getPhysicalWidth() &&
                m.getPhysicalHeight() == mode.getPhysicalHeight()) {
                availableRates.add(rate);
            }
        }
        syncFromSettings();
    }

    private int getSettingOf(String key) {
        float rate = Settings.System.getFloat(context.getContentResolver(), key, 60);
        return availableRates.indexOf(
                Float.valueOf(String.format(Locale.US, "%.02f", rate)));
    }

    private void syncFromSettings() {
        activeRateMin = getSettingOf(KEY_MIN_REFRESH_RATE);
        activeRateMax = getSettingOf(KEY_PEAK_REFRESH_RATE);

        // Check if the rates are valid
        if (activeRateMin == -1 || activeRateMax == -1) {
            isAdaptiveRateActive = true; // Set the flag for adaptive rate
        } else {
            isAdaptiveRateActive = false; // Reset the flag if valid rates are found
        }
    }

    private void cycleRefreshRate() {
        if (isAdaptiveRateActive) return;
        if (activeRateMin < availableRates.size() - 1) {
            activeRateMin++;
        } else {
            activeRateMin = 0;
        }

        float rate = availableRates.get(activeRateMin);
        Settings.System.putFloat(context.getContentResolver(), KEY_MIN_REFRESH_RATE, rate);
        Settings.System.putFloat(context.getContentResolver(), KEY_PREFERRED_REFRESH_RATE, rate);
        Settings.System.putFloat(context.getContentResolver(), KEY_PEAK_REFRESH_RATE, rate);
    }

    private String getFormatRate(float rate) {
        return String.format("%.02f Hz", rate)
                            .replaceAll("[\\.,]00", "");
    }

    private void updateTileView() {
        String displayText;

        if (isAdaptiveRateActive) {
            displayText = "Adaptive";
        } else {

        float min = availableRates.get(activeRateMin);
        float max = availableRates.get(activeRateMax);

        displayText = String.format(Locale.US, min == max ? "%s" : "%s - %s",
            getFormatRate(min), getFormatRate(max));
        }
        
        tile.setContentDescription(displayText);
        tile.setSubtitle(displayText);
        tile.setState(!isAdaptiveRateActive ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        tile = getQsTile();
        syncFromSettings();
        updateTileView();
    }

    @Override
    public void onClick() {
        if (isAdaptiveRateActive) return;
        super.onClick();
        cycleRefreshRate();
        syncFromSettings();
        updateTileView();
    }
}
