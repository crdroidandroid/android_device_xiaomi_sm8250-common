/*
 * Copyright (C) 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Display;
import android.view.Surface;

import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

import java.util.NoSuchElementException;

import vendor.xiaomi.hardware.touchfeature.V1_0.ITouchFeature;

public final class ThermalUtils {
    private static final String TAG = "ThermalUtils";

    protected static final int STATE_DEFAULT = 0;

    private static final String THERMAL_CONTROL = "thermal_control";
    private static final String PREF_BASE_SCONFIG = "thermal_base_sconfig";

    // Keep historical buckets in place; only append new ones.
    private static final String[] PROFILE_PREFIXES = {
            "thermal.benchmark=",
            "thermal.browser=",
            "thermal.camera=",
            "thermal.dialer=",
            "thermal.gaming=",
            "thermal.streaming=",
            "thermal.navigation=",
            "thermal.alt_gaming=",
            "thermal.per_normal=",
            "thermal.per_class0=",
            "thermal.per_navigation=",
            "thermal.per_video=",
            "thermal.normal="
    };

    private static final String THERMAL_SCONFIG =
            "/sys/class/thermal/thermal_message/sconfig";
    private static final String INDIA_MAP = "/vendor/etc/thermal-map-india.conf";
    private static final String PROP_THERMAL_MAP = "persist.vendor.thermal.map";
    private static final String MAP_GLOBAL = "global";
    private static final String MAP_INDIA = "india";

    private final Context mContext;
    private final SharedPreferences mSharedPrefs;
    private final Display mDisplay;
    private ITouchFeature mTouchFeature;
    private boolean mTouchModeChanged;

    protected ThermalUtils(Context context) {
        mContext = context.getApplicationContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        DisplayManager displayManager = mContext.getSystemService(DisplayManager.class);
        mDisplay = displayManager == null ? null
                : displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        mTouchFeature = getTouchFeature();
    }

    public static void startService(Context context) {
        ensureRegion();
        if (FileUtils.isFileWritable(THERMAL_SCONFIG)) {
            context.startService(new Intent(context, ThermalService.class));
        }
    }

    static void ensureRegion() {
        String map = SystemProperties.get(PROP_THERMAL_MAP, "");
        if (MAP_GLOBAL.equals(map)) return;
        if (MAP_INDIA.equals(map) && isIndiaMapAvailable()) return;

        String hwVersion = SystemProperties.get("ro.boot.hwversion", "");
        String initial = isIndiaHardware(hwVersion) && isIndiaMapAvailable()
                ? MAP_INDIA : MAP_GLOBAL;
        try {
            SystemProperties.set(PROP_THERMAL_MAP, initial);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot initialize thermal map selection", e);
        }
    }

    private static boolean isIndiaHardware(String hwVersion) {
        return hwVersion.startsWith("9.21")
                || hwVersion.startsWith("9.22")
                || hwVersion.startsWith("9.29");
    }

    static boolean isIndiaMapAvailable() {
        return FileUtils.fileExists(INDIA_MAP);
    }

    static int getSelectedRegion() {
        String map = SystemProperties.get(PROP_THERMAL_MAP, "");
        if (MAP_INDIA.equals(map) && isIndiaMapAvailable()) {
            return ThermalProfiles.REGION_INDIA;
        }
        return ThermalProfiles.REGION_GLOBAL;
    }

    protected boolean setSelectedRegion(int region) {
        if (region == ThermalProfiles.REGION_INDIA && !isIndiaMapAvailable()) {
            return false;
        }
        String value = region == ThermalProfiles.REGION_INDIA ? MAP_INDIA : MAP_GLOBAL;
        try {
            SystemProperties.set(PROP_THERMAL_MAP, value);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot change thermal map", e);
            return false;
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> startService(mContext), 350);
        return value.equals(SystemProperties.get(PROP_THERMAL_MAP, ""));
    }

    protected int getBaseSconfig() {
        int sconfig = mSharedPrefs.getInt(PREF_BASE_SCONFIG, 0);
        return ThermalProfiles.findBySconfig(getSelectedRegion(), sconfig) != null ? sconfig : 0;
    }

    protected boolean setBaseSconfig(int sconfig) {
        if (ThermalProfiles.findBySconfig(getSelectedRegion(), sconfig) == null) return false;
        mSharedPrefs.edit().putInt(PREF_BASE_SCONFIG, sconfig).apply();
        startService(mContext);
        return true;
    }

    private ITouchFeature getTouchFeature() {
        try {
            return ITouchFeature.getService();
        } catch (RemoteException | NoSuchElementException e) {
            Log.d(TAG, "Touch feature HAL is unavailable");
            return null;
        }
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(THERMAL_CONTROL, profiles).apply();
    }

    private static boolean hasValidProfilePrefixes(String[] modes) {
        if (modes.length == 0 || modes.length > PROFILE_PREFIXES.length) return false;
        for (int i = 0; i < modes.length; i++) {
            if (!modes[i].startsWith(PROFILE_PREFIXES[i])) return false;
        }
        return true;
    }

    private static String appendProfiles(String value, int from, int to) {
        StringBuilder result = new StringBuilder(value);
        for (int i = from; i < to; i++) result.append(':').append(PROFILE_PREFIXES[i]);
        return result.toString();
    }

    private String getValue() {
        String value = mSharedPrefs.getString(THERMAL_CONTROL, null);
        if (value != null && !value.isEmpty()) {
            String[] modes = value.split(":", -1);
            if (hasValidProfilePrefixes(modes)) {
                if (modes.length < PROFILE_PREFIXES.length) {
                    value = appendProfiles(value, modes.length, PROFILE_PREFIXES.length);
                    writeValue(value);
                }
            } else {
                value = null;
            }
        }
        if (value == null || value.isEmpty()) {
            value = String.join(":", PROFILE_PREFIXES);
            writeValue(value);
        }
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
        if (packageName == null || packageName.isEmpty()) return profile;
        int separator = profile.indexOf('=');
        if (separator < 0) return profile;
        StringBuilder result = new StringBuilder(profile.substring(0, separator + 1));
        for (String entry : profile.substring(separator + 1).split(",")) {
            if (!entry.isEmpty() && !packageName.equals(entry)) result.append(entry).append(',');
        }
        return result.toString();
    }

    protected void writePackage(String packageName, int state) {
        if (packageName == null || packageName.isEmpty()) return;
        if (state != STATE_DEFAULT
                && ThermalProfiles.findByStorageState(getSelectedRegion(), state) == null) return;

        String[] modes = getValue().split(":", -1);
        for (int i = 0; i < modes.length; i++) modes[i] = removePackage(modes[i], packageName);
        if (state > STATE_DEFAULT && state <= PROFILE_PREFIXES.length) {
            modes[state - 1] += packageName + ",";
        }
        writeValue(String.join(":", modes));
        startService(mContext);
    }

    protected int getStateForPackage(String packageName) {
        String[] modes = getValue().split(":", -1);
        for (int i = 0; i < modes.length; i++) {
            if (hasPackage(modes[i], packageName)) return i + 1;
        }
        return STATE_DEFAULT;
    }

    private int sconfigForState(int state) {
        if (state == STATE_DEFAULT) return getBaseSconfig();
        ThermalProfiles.Profile profile =
                ThermalProfiles.findByStorageState(getSelectedRegion(), state);
        return profile == null ? getBaseSconfig() : profile.sconfig;
    }

    private void writeThermalState(int state) {
        if (!FileUtils.writeLine(THERMAL_SCONFIG, Integer.toString(state))) {
            Log.w(TAG, "Cannot write thermal state " + state);
        }
    }

    protected void setDefaultThermalProfile() {
        writeThermalState(getBaseSconfig());
    }

    protected void setThermalProfile(String packageName) {
        writeThermalState(sconfigForState(getStateForPackage(packageName)));
        updateTouchModes(packageName);
    }

    private void updateTouchModes(String packageName) {
        String values = mSharedPrefs.getString(packageName, null);
        resetTouchModes();
        if (values == null || values.isEmpty()) return;
        if (mTouchFeature == null) mTouchFeature = getTouchFeature();
        if (mTouchFeature == null) return;

        String[] value = values.split(",", -1);
        if (value.length != 4) return;
        final int gameMode;
        final int touchResponse;
        final int touchSensitivity;
        final int touchResistant;
        try {
            gameMode = Integer.parseInt(value[Constants.TOUCH_GAME_MODE]);
            touchResponse = Integer.parseInt(value[Constants.TOUCH_RESPONSE]);
            touchSensitivity = Integer.parseInt(value[Constants.TOUCH_SENSITIVITY]);
            touchResistant = Integer.parseInt(value[Constants.TOUCH_RESISTANT]);
        } catch (NumberFormatException e) {
            return;
        }

        int touchActiveMode =
                (touchResponse != 0 && touchSensitivity != 0 && touchResistant != 0) ? 1 : 0;
        try {
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_TOLERANCE, touchSensitivity);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_UP_THRESHOLD, touchResponse);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_EDGE_FILTER, touchResistant);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_GAME_MODE, gameMode);
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_ACTIVE_MODE, touchActiveMode);
            mTouchModeChanged = true;
            updateTouchRotation();
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot apply touch profile", e);
            resetTouchModesBestEffort();
            mTouchFeature = null;
            mTouchModeChanged = false;
        }
    }

    private void resetTouchModeBestEffort(int mode) {
        if (mTouchFeature == null) return;
        try {
            mTouchFeature.resetTouchMode(mode);
        } catch (RemoteException e) {
            Log.d(TAG, "Cannot reset touch mode " + mode, e);
        }
    }

    private void resetTouchModesBestEffort() {
        resetTouchModeBestEffort(Constants.MODE_TOUCH_GAME_MODE);
        resetTouchModeBestEffort(Constants.MODE_TOUCH_ACTIVE_MODE);
        resetTouchModeBestEffort(Constants.MODE_TOUCH_UP_THRESHOLD);
        resetTouchModeBestEffort(Constants.MODE_TOUCH_TOLERANCE);
        resetTouchModeBestEffort(Constants.MODE_TOUCH_EDGE_FILTER);
        resetTouchModeBestEffort(Constants.MODE_TOUCH_ROTATION);
    }

    protected void resetTouchModes() {
        if (!mTouchModeChanged) return;
        resetTouchModesBestEffort();
        mTouchModeChanged = false;
    }

    protected void updateTouchRotation() {
        if (!mTouchModeChanged || mDisplay == null || mTouchFeature == null) return;
        final int touchRotation;
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_90: touchRotation = 1; break;
            case Surface.ROTATION_180: touchRotation = 2; break;
            case Surface.ROTATION_270: touchRotation = 3; break;
            case Surface.ROTATION_0:
            default: touchRotation = 0; break;
        }
        try {
            mTouchFeature.setTouchMode(Constants.MODE_TOUCH_ROTATION, touchRotation);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot update touch rotation", e);
            mTouchFeature = null;
            mTouchModeChanged = false;
        }
    }
}
