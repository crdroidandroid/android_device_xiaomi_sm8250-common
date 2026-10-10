/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2020 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.lineageos.settings.dirac.DiracUtils
import org.lineageos.settings.display.DisplayUtils
import org.lineageos.settings.refreshrate.RefreshUtils
import org.lineageos.settings.thermal.ThermalUtils
import org.lineageos.settings.touchsampling.TouchSamplingUtils

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        try {
            DiracUtils.getInstance(context)
        } catch (error: RuntimeException) {
            Log.w("XiaomiParts", "Cannot initialize MiSound", error)
        }
        ThermalUtils.startService(context)
        RefreshUtils.startService(context)
        TouchSamplingUtils.restoreSamplingValue(context)
        DisplayUtils.restore(context)
    }
}
