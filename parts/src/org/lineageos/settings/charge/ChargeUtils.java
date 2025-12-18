/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.charge;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;
import androidx.preference.PreferenceManager;
import org.lineageos.settings.utils.FileUtils;

public class ChargeUtils {

    private static final String TAG = "ChargeUtils";

    public static final String BYPASS_CHARGE_NODE = "/sys/class/power_supply/battery/input_suspend";
    private static final String BATTERY_TEMP_NODE = "/sys/class/power_supply/battery/temp";
    private static final String BATTERY_CAPACITY_NODE = "/sys/class/power_supply/battery/capacity";

    private static final int MAX_BATTERY_TEMP = 450;
    private static final int MIN_BATTERY_CAPACITY = 20;

    private static final String PREF_BYPASS_CHARGE = "bypass_charge";

    public static final int BYPASS_DISABLED = 0;
    public static final int BYPASS_ENABLED = 1;

    private final Context context;
    private final SharedPreferences sharedPrefs;

    public ChargeUtils(Context context) {
        this.context = context;
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public boolean isBypassChargeEnabled() {
        String value = FileUtils.readOneLine(BYPASS_CHARGE_NODE);
        return "1".equals(value);
    }

    public void enableBypassCharge(boolean enable) {
        if (enable) {
            SafetyCheckResult safetyCheck = performSafetyChecks();
            if (!safetyCheck.isSafe()) {
                Log.w(TAG, "Safety check failed: " + safetyCheck.getReason());
                return;
            }
        }

        if (FileUtils.writeLine(BYPASS_CHARGE_NODE, enable ? "1" : "0")) {
            sharedPrefs.edit().putBoolean(PREF_BYPASS_CHARGE, enable).apply();
        } else {
            Log.e(TAG, "Failed to write bypass charge status");
        }
    }

    private boolean isNodeAccessible(String node) {
        return FileUtils.isFileReadable(node) && FileUtils.isFileWritable(node);
    }

    public boolean isBypassChargeSupported() {
        return isNodeAccessible(BYPASS_CHARGE_NODE);
    }

    public SafetyCheckResult performSafetyChecks() {
        if (!isBypassChargeSupported()) {
            return new SafetyCheckResult(false, "Bypass charging not supported on this device");
        }

        if (!isACChargerConnected()) {
            return new SafetyCheckResult(false, "AC charger not connected");
        }

        int batteryTemp = getBatteryTemperature();
        if (batteryTemp >= MAX_BATTERY_TEMP) {
            return new SafetyCheckResult(false, 
                String.format("Battery temperature too high (%.1f°C)", batteryTemp / 10.0f));
        }

        int batteryLevel = getBatteryCapacity();
        if (batteryLevel < MIN_BATTERY_CAPACITY) {
            return new SafetyCheckResult(false, 
                String.format("Battery level too low (%d%%)", batteryLevel));
        }

        return new SafetyCheckResult(true, "All safety checks passed");
    }

    private boolean isACChargerConnected() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (batteryStatus == null) {
            return false;
        }

        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean isCharging = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        Log.d(TAG, "Charger status - plugged: " + chargePlug + ", isAC: " + isCharging);
        return isCharging;
    }

    private int getBatteryTemperature() {
        String tempStr = FileUtils.readOneLine(BATTERY_TEMP_NODE);
        if (tempStr != null) {
            try {
                return Integer.parseInt(tempStr.trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse battery temperature from sysfs", e);
            }
        }

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (batteryStatus != null) {
            int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            Log.d(TAG, "Battery temperature from BatteryManager: " + temp);
            return temp;
        }

        Log.w(TAG, "Unable to read battery temperature");
        return 0;
    }

    private int getBatteryCapacity() {
        String capacityStr = FileUtils.readOneLine(BATTERY_CAPACITY_NODE);
        if (capacityStr != null) {
            try {
                return Integer.parseInt(capacityStr.trim());
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse battery capacity from sysfs", e);
            }
        }

        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            int level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            Log.d(TAG, "Battery capacity from BatteryManager: " + level);
            return level;
        }

        Log.w(TAG, "Unable to read battery capacity");
        return 0;
    }

    public static class SafetyCheckResult {
        private final boolean safe;
        private final String reason;

        public SafetyCheckResult(boolean safe, String reason) {
            this.safe = safe;
            this.reason = reason;
        }

        public boolean isSafe() {
            return safe;
        }

        public String getReason() {
            return reason;
        }
    }
}
