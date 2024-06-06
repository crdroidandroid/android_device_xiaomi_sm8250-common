/*
 * Copyright (C) 2020 The LineageOS Project
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

package org.lineageos.settings.refreshrate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.UserHandle;
import android.view.Display;

import android.provider.Settings;
import android.util.Log;
import android.view.OrientationEventListener;
import android.content.res.Configuration;
import androidx.preference.PreferenceManager;

public final class RefreshUtils {

    private static final String REFRESH_CONTROL = "refresh_control";

    private static float defaultMaxRate;
    private static float defaultMinRate;
    private static final String KEY_PEAK_REFRESH_RATE = "peak_refresh_rate";
    private static final String KEY_MIN_REFRESH_RATE = "min_refresh_rate";
    private Context mContext;
    protected static boolean isAppInList = false;

    protected static final int STATE_DEFAULT = 0;
    protected static final int STATE_STANDARD = 1;
    protected static final int STATE_EXTREME = 2;
    protected static final int STATE_LAND = 3;

    private static final float REFRESH_STATE_DEFAULT = 120f;
    private static final float REFRESH_STATE_STANDARD = 60f;
    private static final float REFRESH_STATE_EXTREME = 120f;
    private static final float REFRESH_STATE_LAND = 60f;

    private static final String REFRESH_STANDARD = "refresh.standard=";
    private static final String REFRESH_EXTREME = "refresh.extreme=";
    private static final String REFRESH_LAND = "refresh.land=";

    private SharedPreferences mSharedPrefs;

    private OrientationEventListener orientationListener;
    private boolean isLandscape = false;

    protected RefreshUtils(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mContext = context;
    }

    public static void startService(Context context) {
        context.startServiceAsUser(new Intent(context, RefreshService.class),
                UserHandle.CURRENT);
    }

    private void writeValue(String profiles) {
        mSharedPrefs.edit().putString(REFRESH_CONTROL, profiles).apply();
    }

   protected void getOldRate(){
        defaultMaxRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, REFRESH_STATE_DEFAULT);
        defaultMinRate = Settings.System.getFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, REFRESH_STATE_DEFAULT);
    }


    private void initializeOrientationListener(String packageName) {
        if (orientationListener != null) {
            orientationListener.disable();
        }

        orientationListener = new OrientationEventListener(mContext) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }

                // Check the device orientation using Configuration
                int currentOrientation = mContext.getResources().getConfiguration().orientation;
                boolean newIsLandscape = (currentOrientation == Configuration.ORIENTATION_LANDSCAPE);
                if (newIsLandscape != isLandscape) {
                    isLandscape = newIsLandscape;
                    adjustRefreshRateForOrientation(packageName);
                }
            }
        };

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        } else {
            orientationListener.disable();
        }
    }

    private void adjustRefreshRateForOrientation(String packageName) {
        float minrate = defaultMinRate;
        float maxrate = isLandscape ? REFRESH_STATE_LAND : REFRESH_STATE_EXTREME;
        if (minrate > maxrate){
            minrate = maxrate;
        }
        Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxrate);
    }


    private String getValue() {
        String value = mSharedPrefs.getString(REFRESH_CONTROL, null);

        if (value == null || value.isEmpty()) {
            value = REFRESH_STANDARD + ":" + REFRESH_EXTREME + ":" + REFRESH_LAND;
            writeValue(value);
        }

        String[] modes = value.split(":");
        if (modes.length < 3) {
            modes = new String[] {
                modes.length > 0 ? modes[0] : REFRESH_STANDARD,
                modes.length > 1 ? modes[1] : REFRESH_EXTREME,
                modes.length > 2 ? modes[2] : REFRESH_LAND
            };
            value = String.join(":", modes);
            writeValue(value);
        }
        return value;
    }

    protected void writePackage(String packageName, int mode) {
        String value = getValue();
        value = value.replace(packageName + ",", "");
        String[] modes = value.split(":");
        String finalString;

        switch (mode) {
            case STATE_STANDARD:
                modes[0] = modes[0] + packageName + ",";
                break;
            case STATE_EXTREME:
                modes[1] = modes[1] + packageName + ",";
                break;
            case STATE_LAND:
                modes[2] = modes[2] + packageName + ",";
                break;

        }

        finalString = modes[0] + ":" + modes[1] + ":" + modes[2];

        writeValue(finalString);
    }

    protected int getStateForPackage(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        int state = STATE_DEFAULT;
        if (modes[0].contains(packageName + ",")) {
            state = STATE_STANDARD;
        } else if (modes[1].contains(packageName + ",")) {
            state = STATE_EXTREME;
        } else if (modes[2].contains(packageName + ",")) {
            state = STATE_LAND;
        }
        return state;
    }

    protected void setRefreshRate(String packageName) {
        String value = getValue();
        String[] modes = value.split(":");
        float maxrate = defaultMaxRate;
        float minrate = defaultMinRate;
        isAppInList = false;

            if (value != null) {
            modes = value.split(":");

            if (modes[0].contains(packageName + ",")) {
                maxrate = REFRESH_STATE_STANDARD;
                if ( minrate > maxrate){
                minrate = maxrate;
                }
		isAppInList = true;
           } else if (modes[1].contains(packageName + ",")) {
                maxrate = REFRESH_STATE_EXTREME;
                if ( minrate > maxrate){
                minrate = maxrate;
                }
		isAppInList = true;
            } else if (modes[2].contains(packageName + ",")) {
                initializeOrientationListener(packageName); // Initialize orientation listener for landscape mode
                isAppInList = true;
                return; // Return early as the listener will handle the refresh rate
            }
          }
	Settings.System.putFloat(mContext.getContentResolver(), KEY_MIN_REFRESH_RATE, minrate);
        Settings.System.putFloat(mContext.getContentResolver(), KEY_PEAK_REFRESH_RATE, maxrate);
    }
}
