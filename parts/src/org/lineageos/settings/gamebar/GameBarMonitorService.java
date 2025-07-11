/*
 * Copyright (C) 2025 kenway214
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

package org.lineageos.settings.gamebar;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import androidx.preference.PreferenceManager;
import java.util.HashSet;
import java.util.Set;

public class GameBarMonitorService extends Service {

    private Handler mHandler;
    private Runnable mMonitorRunnable;
    private static final long MONITOR_INTERVAL = 2000; // 2 seconds
    private volatile boolean mIsRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(android.os.Looper.getMainLooper());
        mMonitorRunnable = new Runnable() {
            @Override
            public void run() {
                if (mIsRunning) {
                    monitorForegroundApp();
                    if (mIsRunning && mHandler != null) {
                        mHandler.postDelayed(this, MONITOR_INTERVAL);
                    }
                }
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsRunning) {
            mIsRunning = true;
            if (mHandler != null && mMonitorRunnable != null) {
                mHandler.post(mMonitorRunnable);
            }
        }
        return START_STICKY;
    }

    private String mLastForegroundApp = "";
    private boolean mLastGameBarState = false;
    
    private void monitorForegroundApp() {
        try {
            if (!mIsRunning) return;
            
            var prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean masterEnabled = prefs.getBoolean("game_bar_enable", false);
            
            if (masterEnabled) {
                if (!mLastGameBarState) {
                    GameBar gameBar = GameBar.getInstance(this);
                    gameBar.applyPreferences();
                    gameBar.show();
                    mLastGameBarState = true;
                }
                return;
            }
            
            boolean autoEnabled = prefs.getBoolean("game_bar_auto_enable", false);
            if (!autoEnabled) {
                if (mLastGameBarState) {
                    GameBar.getInstance(this).hide();
                    mLastGameBarState = false;
                }
                return;
            }
            
            String foreground = ForegroundAppDetector.getForegroundPackageName(this);
            
            // Only update if foreground app changed
            if (!foreground.equals(mLastForegroundApp)) {
                Set<String> autoApps = prefs.getStringSet(
                    org.lineageos.settings.gamebar.GameBarPerAppConfigFragment.PREF_AUTO_APPS, 
                    new HashSet<>());
                    
                boolean shouldShow = autoApps.contains(foreground);
                
                if (shouldShow && !mLastGameBarState) {
                    GameBar gameBar = GameBar.getInstance(this);
                    gameBar.applyPreferences();
                    gameBar.show();
                    mLastGameBarState = true;
                } else if (!shouldShow && mLastGameBarState) {
                    GameBar.getInstance(this).hide();
                    mLastGameBarState = false;
                }
                
                mLastForegroundApp = foreground;
            }
        } catch (Exception e) {
            // Prevent crashes from propagating
            android.util.Log.e("GameBarMonitorService", "Error in monitorForegroundApp", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRunning = false;
        
        if (mHandler != null) {
            mHandler.removeCallbacks(mMonitorRunnable);
            mHandler.removeCallbacksAndMessages(null);
        }
        
        // Clean up GameBar instance
        try {
            GameBar.destroyInstance();
        } catch (Exception e) {
            android.util.Log.e("GameBarMonitorService", "Error destroying GameBar instance", e);
        }
        
        // Clear state variables to prevent lingering references
        mLastForegroundApp = "";
        mLastGameBarState = false;
        mHandler = null;
        mMonitorRunnable = null;
    }
}
