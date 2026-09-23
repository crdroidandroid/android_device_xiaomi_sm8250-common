/*
 * Copyright (C) 2021 crDroid Android Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings;

import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.view.Display;
import android.widget.Toast;

import org.lineageos.settings.refreshrate.RefreshUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public class RefreshRateTileService extends TileService {
    private final List<Float> mAvailableRates = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        loadAvailableRates();
    }

    private void loadAvailableRates() {
        mAvailableRates.clear();
        DisplayManager manager = getSystemService(DisplayManager.class);
        Display display = manager == null ? null : manager.getDisplay(Display.DEFAULT_DISPLAY);
        if (display == null) return;

        Display.Mode current = display.getMode();
        LinkedHashSet<Float> rates = new LinkedHashSet<>();
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() == current.getPhysicalWidth()
                    && mode.getPhysicalHeight() == current.getPhysicalHeight()) {
                rates.add(roundRate(mode.getRefreshRate()));
            }
        }
        mAvailableRates.addAll(rates);
        Collections.sort(mAvailableRates);
    }

    private static float roundRate(float rate) {
        return Float.parseFloat(String.format(Locale.US, "%.02f", rate));
    }

    private int nearestRateIndex(float value) {
        if (mAvailableRates.isEmpty()) return -1;
        int best = 0;
        float distance = Math.abs(mAvailableRates.get(0) - value);
        for (int i = 1; i < mAvailableRates.size(); i++) {
            float next = Math.abs(mAvailableRates.get(i) - value);
            if (next < distance) {
                best = i;
                distance = next;
            }
        }
        return best;
    }

    private float readRate(String key, float fallback) {
        return Settings.System.getFloat(getContentResolver(), key, fallback);
    }

    private boolean writeRate(float rate) {
        float oldMin = readRate(Settings.System.MIN_REFRESH_RATE, rate);
        float oldPeak = readRate(Settings.System.PEAK_REFRESH_RATE, rate);

        if (!Settings.System.putFloat(getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, rate)) {
            return false;
        }
        if (!Settings.System.putFloat(getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, rate)) {
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.MIN_REFRESH_RATE, oldMin);
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.PEAK_REFRESH_RATE, oldPeak);
            return false;
        }
        return true;
    }

    private String formatRate(float rate) {
        return String.format(Locale.US, "%.02f Hz", rate).replaceAll("[\\.,]00", "");
    }

    private void updateTileView() {
        Tile tile = getQsTile();
        if (tile == null) return;
        if (mAvailableRates.isEmpty()) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.updateTile();
            return;
        }

        int minIndex = nearestRateIndex(readRate(
                Settings.System.MIN_REFRESH_RATE, mAvailableRates.get(0)));
        int maxIndex = nearestRateIndex(readRate(
                Settings.System.PEAK_REFRESH_RATE,
                mAvailableRates.get(mAvailableRates.size() - 1)));
        float min = mAvailableRates.get(minIndex);
        float max = mAvailableRates.get(maxIndex);
        String displayText = min == max ? formatRate(min)
                : formatRate(min) + " - " + formatRate(max);

        tile.setContentDescription(displayText);
        tile.setSubtitle(displayText);
        tile.setState(min == max ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        if (mAvailableRates.isEmpty()) loadAvailableRates();
        updateTileView();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (mAvailableRates.isEmpty()) {
            updateTileView();
            return;
        }

        float current = readRate(Settings.System.MIN_REFRESH_RATE, mAvailableRates.get(0));
        int index = nearestRateIndex(current);
        float next = mAvailableRates.get((index + 1) % mAvailableRates.size());
        if (!writeRate(next)) {
            Toast.makeText(this, R.string.parts_apply_failed, Toast.LENGTH_SHORT).show();
        } else {
            RefreshUtils.updateUserBaseline(this, next, next);
        }
        updateTileView();
    }
}
