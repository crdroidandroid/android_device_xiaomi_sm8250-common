/*
 * Copyright (C) 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.refreshrate;

import android.app.ActivityTaskManager;
import android.app.ActivityTaskManager.RootTaskInfo;
import android.app.IActivityTaskManager;
import android.app.Service;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

public class RefreshService extends Service {
    private static final String TAG = "RefreshService";

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mDestroyed;
    private boolean mScreenOn;
    private boolean mReceiverRegistered;
    private boolean mTaskListenerRegistered;
    private String mPreviousApp = "";

    private RefreshUtils mRefreshUtils;
    private IActivityTaskManager mActivityTaskManager;

    private final Runnable mRefreshForeground = () -> refreshForegroundApp(false);

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDestroyed || intent == null) return;
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mScreenOn = false;
                mPreviousApp = "";
                mRefreshUtils.restoreDefaultRates();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mScreenOn = true;
                refreshForegroundApp(true);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        mRefreshUtils = new RefreshUtils(this);
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

        mRefreshUtils.restoreDefaultRates();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void refreshForegroundApp(boolean force) {
        if (mDestroyed) return;
        if (!mScreenOn) {
            mPreviousApp = "";
            mRefreshUtils.restoreDefaultRates();
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
                mPreviousApp = "";
                mRefreshUtils.restoreDefaultRates();
                return;
            }
        }

        if (!force && foregroundApp.equals(mPreviousApp)) return;
        mPreviousApp = foregroundApp;
        if (foregroundApp.isEmpty()) {
            mRefreshUtils.restoreDefaultRates();
        } else {
            mRefreshUtils.setRefreshRate(foregroundApp);
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
