/*
 * Copyright (C) 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.refreshrate

import android.app.*
import android.content.*
import android.os.*
import android.util.Log

class RefreshService : android.app.Service() {
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var destroyed = false
    private var screenOn = false
    private var receiverRegistered = false
    private var listenerRegistered = false
    private var previous = ""
    private lateinit var utils: RefreshUtils
    private var tasks: IActivityTaskManager? = null
    private val refresh = Runnable { foreground(false) }
    private val listener =
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                if (!destroyed) {
                    handler.removeCallbacks(refresh)
                    handler.post(refresh)
                }
            }
        }
    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (destroyed) return
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        screenOn = false
                        previous = ""
                        utils.restoreDefaultRates()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        screenOn = true
                        foreground(true)
                    }
                }
            }
        }

    override fun onCreate() {
        super.onCreate()
        utils = RefreshUtils(this)
        tasks = ActivityTaskManager.getService()
        screenOn = getSystemService(PowerManager::class.java)?.isInteractive == true
        registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            Context.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
        try {
            tasks?.let {
                it.registerTaskStackListener(listener)
                listenerRegistered = true
            }
        } catch (error: Exception) {
            Log.w("RefreshService", "Cannot register task listener", error)
        }
        foreground(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreground(true)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        destroyed = true
        handler.removeCallbacks(refresh)
        if (receiverRegistered)
            try {
                unregisterReceiver(receiver)
            } catch (error: RuntimeException) {
                Log.w("RefreshService", "Cannot unregister receiver", error)
            }
        if (listenerRegistered)
            try {
                tasks?.unregisterTaskStackListener(listener)
            } catch (error: Exception) {
                Log.w("RefreshService", "Cannot unregister task listener", error)
            }
        utils.restoreDefaultRates()
        super.onDestroy()
    }

    private fun foreground(force: Boolean) {
        if (destroyed) return
        if (!screenOn) {
            previous = ""
            utils.restoreDefaultRates()
            return
        }
        val packageName =
            try {
                tasks?.focusedRootTaskInfo?.topActivity?.packageName ?: ""
            } catch (error: Exception) {
                Log.w("RefreshService", "Cannot query foreground", error)
                previous = ""
                utils.restoreDefaultRates()
                return
            }
        if (!force && previous == packageName) return
        previous = packageName
        if (packageName.isEmpty()) utils.restoreDefaultRates()
        else utils.setRefreshRate(packageName)
    }
}
