/*
 * Copyright (C) 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal;

import android.app.ActivityTaskManager;
import android.app.ActivityTaskManager.RootTaskInfo;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

public class ThermalService extends Service {
    private static final String TAG = "ThermalService";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile boolean mDestroyed;
    private boolean mScreenOn;
    private boolean mReceiverRegistered;
    private boolean mTaskListenerRegistered;
    private String mCurrentApp = "";
    private ThermalUtils mThermalUtils;
    private IActivityTaskManager mActivityTaskManager;

    private final Runnable mRefreshForeground = () -> refreshForegroundApp(false);

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDestroyed || intent == null) return;
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mScreenOn = false;
                mCurrentApp = "";
                mThermalUtils.resetTouchModes();
                mThermalUtils.setDefaultThermalProfile();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mScreenOn = true;
                refreshForegroundApp(true);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mThermalUtils = new ThermalUtils(this);
        mActivityTaskManager = ActivityTaskManager.getService();
        PowerManager powerManager = getSystemService(PowerManager.class);
        mScreenOn = powerManager != null && powerManager.isInteractive();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mIntentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        mReceiverRegistered = true;

        if (mActivityTaskManager != null) {
            try {
                mActivityTaskManager.registerTaskStackListener(mTaskListener);
                mTaskListenerRegistered = true;
            } catch (RemoteException | RuntimeException e) {
                Log.w(TAG, "Cannot register task listener", e);
            }
        }
        refreshForegroundApp(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        refreshForegroundApp(true);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        mHandler.removeCallbacks(mRefreshForeground);

        if (mReceiverRegistered) {
            try {
                unregisterReceiver(mIntentReceiver);
            } catch (RuntimeException e) {
                Log.w(TAG, "Cannot unregister screen receiver", e);
            }
            mReceiverRegistered = false;
        }

        if (mTaskListenerRegistered && mActivityTaskManager != null) {
            try {
                mActivityTaskManager.unregisterTaskStackListener(mTaskListener);
            } catch (RemoteException | RuntimeException e) {
                Log.w(TAG, "Cannot unregister task listener", e);
            }
            mTaskListenerRegistered = false;
        }

        mThermalUtils.resetTouchModes();
        mThermalUtils.setDefaultThermalProfile();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!mDestroyed) mThermalUtils.updateTouchRotation();
    }

    private void refreshForegroundApp(boolean force) {
        if (mDestroyed) return;
        if (!mScreenOn) {
            mCurrentApp = "";
            mThermalUtils.resetTouchModes();
            mThermalUtils.setDefaultThermalProfile();
            return;
        }

        String foregroundApp = "";
        if (mActivityTaskManager != null) {
            try {
                RootTaskInfo info = mActivityTaskManager.getFocusedRootTaskInfo();
                if (info != null && info.topActivity != null) {
                    foregroundApp = info.topActivity.getPackageName();
                }
            } catch (RemoteException | RuntimeException e) {
                Log.w(TAG, "Cannot query foreground app", e);
                mCurrentApp = "";
                mThermalUtils.resetTouchModes();
                mThermalUtils.setDefaultThermalProfile();
                return;
            }
        }

        if (!force && foregroundApp.equals(mCurrentApp)) return;
        mCurrentApp = foregroundApp;
        if (mCurrentApp.isEmpty()) {
            mThermalUtils.resetTouchModes();
            mThermalUtils.setDefaultThermalProfile();
        } else {
            mThermalUtils.setThermalProfile(mCurrentApp);
        }
    }

    private final TaskStackListener mTaskListener = new TaskStackListener() {
        @Override
        public void onTaskStackChanged() {
            if (mDestroyed) return;
            mHandler.removeCallbacks(mRefreshForeground);
            mHandler.post(mRefreshForeground);
        }
    };
}
