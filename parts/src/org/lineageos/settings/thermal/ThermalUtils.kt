/*
 * Copyright (C) 2020 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.*
import android.util.Log
import android.view.Display
import org.lineageos.settings.touchsampling.TouchSamplingUtils
import org.lineageos.settings.utils.FileUtils
import org.lineageos.settings.utils.PartsPreferences
import vendor.xiaomi.hardware.touchfeature.V1_0.ITouchFeature

class ThermalUtils(context: Context) {
    companion object {
        const val STATE_DEFAULT = 0
        private const val SCONFIG = "/sys/class/thermal/thermal_message/sconfig"
        private const val MAP = "persist.sys.xiaomi.thermal.map"
        private val PREFIXES =
            listOf(
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
                "thermal.normal=",
            )

        @JvmStatic
        fun startService(context: Context) {
            ensureRegion()
            if (FileUtils.isFileWritable(SCONFIG))
                context.startService(Intent(context, ThermalService::class.java))
        }

        fun ensureRegion() {
            val map = SystemProperties.get(MAP, "")
            if (map == "global" || (map == "india" && isIndiaMapAvailable())) return
            val hw = SystemProperties.get("ro.boot.hwversion", "")
            val india =
                listOf("9.21", "9.22", "9.29").any { hw.startsWith(it) } && isIndiaMapAvailable()
            try {
                SystemProperties.set(MAP, if (india) "india" else "global")
            } catch (error: RuntimeException) {
                Log.w("ThermalUtils", "Cannot initialize thermal map", error)
            }
        }

        fun isIndiaMapAvailable() = FileUtils.fileExists("/vendor/etc/thermal-map-india.conf")

        fun getSelectedRegion() =
            if (SystemProperties.get(MAP, "") == "india" && isIndiaMapAvailable())
                ThermalProfiles.REGION_INDIA
            else ThermalProfiles.REGION_GLOBAL

        private fun touchFeature(): ITouchFeature? =
            try {
                ITouchFeature.getService()
            } catch (error: Exception) {
                Log.d("ThermalUtils", "Touch HAL unavailable", error)
                null
            }
    }

    private val app = context.applicationContext
    private val prefs = PartsPreferences.getDefaultSharedPreferences(app)
    private val display =
        app.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
    private var hal = touchFeature()
    private var touchChanged = false

    fun setSelectedRegion(region: Int): Boolean {
        if (region == ThermalProfiles.REGION_INDIA && !isIndiaMapAvailable()) return false
        val value = if (region == ThermalProfiles.REGION_INDIA) "india" else "global"
        try {
            SystemProperties.set(MAP, value)
        } catch (error: RuntimeException) {
            Log.w("ThermalUtils", "Cannot change thermal map", error)
            return false
        }
        Handler(Looper.getMainLooper()).postDelayed({ startService(app) }, 350)
        return SystemProperties.get(MAP, "") == value
    }

    fun getBaseSconfig(): Int =
        prefs.getInt("thermal_base_sconfig", 0).let {
            if (ThermalProfiles.findBySconfig(getSelectedRegion(), it) != null) it else 0
        }

    fun setBaseSconfig(sconfig: Int): Boolean {
        if (ThermalProfiles.findBySconfig(getSelectedRegion(), sconfig) == null) return false
        prefs.edit().putInt("thermal_base_sconfig", sconfig).apply()
        startService(app)
        return true
    }

    private fun writeValue(value: List<String>) {
        prefs.edit().putString("thermal_control", value.joinToString(":")).apply()
    }

    private fun getValue(): List<String> {
        val saved = prefs.getString("thermal_control", null)?.split(':')
        if (
            saved != null &&
                saved.isNotEmpty() &&
                saved.size <= PREFIXES.size &&
                saved.indices.all { saved[it].startsWith(PREFIXES[it]) }
        ) {
            return (saved + PREFIXES.drop(saved.size)).also {
                if (saved.size != PREFIXES.size) writeValue(it)
            }
        }
        return PREFIXES.also { writeValue(it) }
    }

    fun writePackage(packageName: String, state: Int) {
        if (
            packageName.isEmpty() ||
                (state != STATE_DEFAULT &&
                    ThermalProfiles.findByStorageState(getSelectedRegion(), state) == null)
        )
            return
        val modes =
            getValue()
                .map { profile ->
                    profile.substringBefore('=') +
                        "=" +
                        profile
                            .substringAfter('=')
                            .split(',')
                            .filter { it.isNotEmpty() && it != packageName }
                            .joinToString(",")
                            .let { if (it.isEmpty()) "" else "$it," }
                }
                .toMutableList()
        if (state in 1..PREFIXES.size) modes[state - 1] += "$packageName,"
        writeValue(modes)
        startService(app)
    }

    fun getStateForPackage(packageName: String): Int {
        if (packageName.isEmpty()) return STATE_DEFAULT
        val index = getValue().indexOfFirst { packageName in it.substringAfter('=').split(',') }
        return if (index < 0) STATE_DEFAULT else index + 1
    }

    private fun writeThermalState(state: Int) {
        if (!FileUtils.writeLine(SCONFIG, state.toString()))
            Log.w("ThermalUtils", "Cannot write thermal state $state")
    }

    fun setDefaultThermalProfile() {
        writeThermalState(getBaseSconfig())
    }

    fun setThermalProfile(packageName: String) {
        val state = getStateForPackage(packageName)
        val target =
            if (state == STATE_DEFAULT) getBaseSconfig()
            else
                ThermalProfiles.findByStorageState(getSelectedRegion(), state)?.sconfig
                    ?: getBaseSconfig()
        writeThermalState(target)
        updateTouchModes(packageName)
    }

    private fun updateTouchModes(packageName: String) {
        if (AliothTouchProfile.isSupported() && TouchSamplingUtils.isEnabled(app)) {
            touchChanged = false
            return
        }
        val saved = prefs.getString(packageName, null)
        resetTouchModes()
        if (saved.isNullOrEmpty()) return
        val values = saved.split(',').map { it.toIntOrNull() }
        if (values.size != 4 || values.any { it == null }) return
        if (hal == null) hal = touchFeature()
        val touch = hal ?: return
        val game = values[0]!!
        val response = values[1]!!
        val sensitivity = values[2]!!
        val edge = values[3]!!
        val active =
            if (AliothTouchProfile.isSupported()) game
            else if (response != 0 && sensitivity != 0 && edge != 0) 1 else 0
        try {
            if (AliothTouchProfile.isSupported()) {
                AliothTouchProfile.applyMode(touch, Constants.MODE_TOUCH_TOLERANCE, sensitivity)
                AliothTouchProfile.applyMode(touch, Constants.MODE_TOUCH_UP_THRESHOLD, response)
            } else {
                touch.setTouchMode(Constants.MODE_TOUCH_TOLERANCE, sensitivity)
                touch.setTouchMode(Constants.MODE_TOUCH_UP_THRESHOLD, response)
            }
            touch.setTouchMode(Constants.MODE_TOUCH_EDGE_FILTER, edge)
            touch.setTouchMode(Constants.MODE_TOUCH_GAME_MODE, game)
            touch.setTouchMode(Constants.MODE_TOUCH_ACTIVE_MODE, active)
            if (game == 1) AliothTouchProfile.apply(touch, prefs, packageName)
            touchChanged = true
            updateTouchRotation()
        } catch (error: RemoteException) {
            Log.w("ThermalUtils", "Cannot apply touch profile", error)
            resetBestEffort()
            hal = null
            touchChanged = false
        }
    }

    private fun resetBestEffort() {
        val modes = mutableListOf(0, 1, 2, 3, 7, 8)
        if (AliothTouchProfile.isSupported()) modes.addAll(listOf(4, 5, 6))
        modes.forEach {
            try {
                hal?.resetTouchMode(it)
            } catch (error: RemoteException) {
                Log.d("ThermalUtils", "Cannot reset touch mode $it", error)
            }
        }
    }

    fun resetTouchModes() {
        if (!touchChanged) return
        if (!(AliothTouchProfile.isSupported() && TouchSamplingUtils.isEnabled(app)))
            resetBestEffort()
        touchChanged = false
    }

    fun updateTouchRotation() {
        if (!touchChanged || display == null || hal == null) return
        try {
            hal?.setTouchMode(Constants.MODE_TOUCH_ROTATION, display.rotation)
        } catch (error: RemoteException) {
            Log.w("ThermalUtils", "Cannot update touch rotation", error)
            hal = null
            touchChanged = false
        }
    }
}
