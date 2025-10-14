/*
 * Copyright (C) 2023-2024 Paranoid Android
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.display;

import static android.provider.Settings.System.DISPLAY_COLOR_MODE;
import static org.lineageos.settings.display.DfWrapper.DfParams;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import java.util.Map;

public class ColorModeService extends Service {
    private static final String TAG = "XiaomiPartsColorModeService";
    private static final boolean DEBUG = true;

    private static final int DEFAULT_COLOR_MODE = SystemProperties.getInt(
            "persist.sys.sf.native_mode", 0);

    private static final DfParams STANDARD_PARAMS = new DfParams(2, 2, 255);

    /* original/p3/srgb */
    private static final int EXPERT_MODE = 26;
    private static final DfParams EXPERT_PARAMS = new DfParams(EXPERT_MODE, 0, 10);

    /* color mode -> displayfeature (mode, value, cookie) */
    private static final Map<Integer, DfParams> COLOR_MAP = Map.of(
        258, new DfParams(0, 2, 255),  // Vivid
        256, new DfParams(1, 2, 255),  // Saturated
        257, STANDARD_PARAMS,          // Original Colour Pro
        266, new DfParams(26, 1, 0),   // Original (advanced)
        268, new DfParams(26, 2, 0),   // P3
        267, new DfParams(26, 3, 0)    // sRGB
    );

    private final ContentObserver mSettingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG) Log.d(TAG, "SettingObserver: onChange");
            setCurrentColorMode();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) Log.d(TAG, "onCreate");
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(DISPLAY_COLOR_MODE),
                false, mSettingObserver, UserHandle.USER_CURRENT);
        setCurrentColorMode();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy");
        getContentResolver().unregisterContentObserver(mSettingObserver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setCurrentColorMode() {
        final int colorMode = Settings.System.getIntForUser(getContentResolver(),
                DISPLAY_COLOR_MODE, DEFAULT_COLOR_MODE, UserHandle.USER_CURRENT);
        
        // Use getOrDefault to handle unknown color modes gracefully
        final DfParams params = COLOR_MAP.getOrDefault(colorMode, STANDARD_PARAMS);
        
        if (DEBUG) Log.d(TAG, "setCurrentColorMode: " + colorMode + ", params=" + params);
        
        // Set expert params first if this is an expert mode
        if (params.mode == EXPERT_MODE) {
            DfWrapper.setDisplayFeature(EXPERT_PARAMS);
        }
        DfWrapper.setDisplayFeature(params);
    }
}
