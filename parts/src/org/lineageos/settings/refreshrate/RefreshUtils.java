/*
 * Copyright (C) 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

public final class RefreshUtils {
    private static final String TAG = "RefreshUtils";

    private static final String REFRESH_CONTROL = "refresh_control";
    private static final String REFRESH_STANDARD = "refresh.standard=";
    private static final String REFRESH_EXTREME = "refresh.extreme=";
    private static final String[] PROFILE_PREFIXES = {REFRESH_STANDARD, REFRESH_EXTREME};

    private static final String PREF_OVERRIDE_ACTIVE = "refresh_override_active";
    private static final String PREF_BASE_MIN = "refresh_base_min";
    private static final String PREF_BASE_PEAK = "refresh_base_peak";

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_STANDARD = 1;
    protected static final int STATE_EXTREME = 2;

    private static final float REFRESH_STATE_STANDARD = 60f;
    private static final float REFRESH_STATE_EXTREME = 120f;

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;

    protected RefreshUtils(Context context) {
        mContext = context.getApplicationContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public static void startService(Context context) {
        context.startService(new Intent(context, RefreshService.class));
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(REFRESH_CONTROL, profiles).apply();
    }

    private static boolean validProfiles(String[] profiles) {
        if (profiles.length != PROFILE_PREFIXES.length) return false;
        for (int i = 0; i < profiles.length; i++) {
            if (!profiles[i].startsWith(PROFILE_PREFIXES[i])) return false;
        }
        return true;
    }

    private String getValue() {
        String value = mSharedPrefs.getString(REFRESH_CONTROL, null);
        if (value != null && !value.isEmpty()
                && validProfiles(value.split(":", -1))) {
            return value;
        }

        value = String.join(":", PROFILE_PREFIXES);
        writeValue(value);
        return value;
    }

    private static boolean hasPackage(String profile, String packageName) {
        if (packageName == null || packageName.isEmpty()) return false;
        int separator = profile.indexOf('=');
        if (separator < 0) return false;
        for (String entry : profile.substring(separator + 1).split(",")) {
            if (packageName.equals(entry)) return true;
        }
        return false;
    }

    private static String removePackage(String profile, String packageName) {
        int separator = profile.indexOf('=');
        if (separator < 0 || packageName == null || packageName.isEmpty()) return profile;
        StringBuilder result = new StringBuilder(profile.substring(0, separator + 1));
        for (String entry : profile.substring(separator + 1).split(",")) {
            if (!entry.isEmpty() && !packageName.equals(entry)) {
                result.append(entry).append(',');
            }
        }
        return result.toString();
    }

    protected void writePackage(String packageName, int mode) {
        if (packageName == null || packageName.isEmpty()) return;
        String[] modes = getValue().split(":", -1);
        for (int i = 0; i < modes.length; i++) {
            modes[i] = removePackage(modes[i], packageName);
        }

        if (mode == STATE_STANDARD || mode == STATE_EXTREME) {
            modes[mode - 1] += packageName + ",";
        }
        writeValue(String.join(":", modes));
        startService(mContext);
    }

    protected int getStateForPackage(String packageName) {
        String[] modes = getValue().split(":", -1);
        if (hasPackage(modes[0], packageName)) return STATE_STANDARD;
        if (hasPackage(modes[1], packageName)) return STATE_EXTREME;
        return STATE_DEFAULT;
    }

    private float currentMin() {
        return Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, 0f);
    }

    private float currentPeak() {
        return Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, REFRESH_STATE_EXTREME);
    }

    private boolean writeRates(float min, float peak) {
        float oldMin = currentMin();
        float oldPeak = currentPeak();

        if (!Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, min)) {
            return false;
        }
        if (!Settings.System.putFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, peak)) {
            Settings.System.putFloat(mContext.getContentResolver(),
                    Settings.System.MIN_REFRESH_RATE, oldMin);
            return false;
        }
        return true;
    }

    private void captureBaselineIfNeeded() {
        if (mSharedPrefs.getBoolean(PREF_OVERRIDE_ACTIVE, false)) return;
        mSharedPrefs.edit()
                .putFloat(PREF_BASE_MIN, currentMin())
                .putFloat(PREF_BASE_PEAK, currentPeak())
                .putBoolean(PREF_OVERRIDE_ACTIVE, true)
                .apply();
    }

    protected boolean restoreDefaultRates() {
        if (!mSharedPrefs.getBoolean(PREF_OVERRIDE_ACTIVE, false)) return true;

        float min = mSharedPrefs.getFloat(PREF_BASE_MIN, 0f);
        float peak = mSharedPrefs.getFloat(PREF_BASE_PEAK, REFRESH_STATE_EXTREME);
        if (!writeRates(min, peak)) {
            Log.w(TAG, "Cannot restore user refresh-rate baseline");
            return false;
        }

        mSharedPrefs.edit()
                .remove(PREF_OVERRIDE_ACTIVE)
                .remove(PREF_BASE_MIN)
                .remove(PREF_BASE_PEAK)
                .apply();
        return true;
    }

    public static void updateUserBaseline(Context context, float min, float peak) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                context.getApplicationContext());
        if (prefs.getBoolean(PREF_OVERRIDE_ACTIVE, false)) {
            prefs.edit().putFloat(PREF_BASE_MIN, min).putFloat(PREF_BASE_PEAK, peak).apply();
        }
    }

    protected boolean setRefreshRate(String packageName) {
        int state = getStateForPackage(packageName);
        if (state == STATE_DEFAULT) return restoreDefaultRates();

        captureBaselineIfNeeded();
        float baseMin = mSharedPrefs.getFloat(PREF_BASE_MIN, 0f);
        float targetPeak = state == STATE_STANDARD
                ? REFRESH_STATE_STANDARD : REFRESH_STATE_EXTREME;
        float targetMin = baseMin > targetPeak ? targetPeak : baseMin;

        if (!writeRates(targetMin, targetPeak)) {
            Log.w(TAG, "Cannot apply per-app refresh rate for " + packageName);
            restoreDefaultRates();
            return false;
        }
        return true;
    }
}
