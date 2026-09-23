/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal;

import org.lineageos.settings.R;

final class ThermalProfiles {
    static final int REGION_GLOBAL = 0;
    static final int REGION_INDIA = 1;

    static final int CONTROL_BATTERY = 1 << 0;
    static final int CONTROL_TEMP_STATE = 1 << 1;
    static final int CONTROL_HOTPLUG = 1 << 2;
    static final int CONTROL_BOOST_LIMIT = 1 << 3;
    static final int CONTROL_LOW_BATTERY = 1 << 4;
    static final int CONTROL_HBM = 1 << 5;
    static final int CONTROL_BACKLIGHT = 1 << 6;
    static final int CONTROL_MODEM = 1 << 7;
    static final int CONTROL_WIRELESS = 1 << 8;

    static final class Policy {
        final String configName;
        final int[] cpu4Trigger, cpu4Clear, cpu4Khz;
        final int[] cpu7Trigger, cpu7Clear, cpu7Khz;
        final int[] gpuTrigger, gpuClear, gpuState;
        final int[] batteryTrigger, tempStateTrigger;
        final int controls;
        final boolean definesVirtualSensor;

        Policy(String configName, int[] cpu4Trigger, int[] cpu4Clear, int[] cpu4Khz,
                int[] cpu7Trigger, int[] cpu7Clear, int[] cpu7Khz,
                int[] gpuTrigger, int[] gpuClear, int[] gpuState,
                int[] batteryTrigger, int[] tempStateTrigger,
                int controls, boolean definesVirtualSensor) {
            this.configName = configName;
            this.cpu4Trigger = cpu4Trigger;
            this.cpu4Clear = cpu4Clear;
            this.cpu4Khz = cpu4Khz;
            this.cpu7Trigger = cpu7Trigger;
            this.cpu7Clear = cpu7Clear;
            this.cpu7Khz = cpu7Khz;
            this.gpuTrigger = gpuTrigger;
            this.gpuClear = gpuClear;
            this.gpuState = gpuState;
            this.batteryTrigger = batteryTrigger;
            this.tempStateTrigger = tempStateTrigger;
            this.controls = controls;
            this.definesVirtualSensor = definesVirtualSensor;
        }

        boolean hasControl(int control) {
            return (controls & control) != 0;
        }

        String compactConfigName() {
            String name = configName;
            if (name.startsWith("thermal-india-")) {
                name = name.substring("thermal-india-".length());
            } else if (name.startsWith("thermal-")) {
                name = name.substring("thermal-".length());
            }
            if (name.endsWith(".conf")) {
                name = name.substring(0, name.length() - ".conf".length());
            }
            return name;
        }
    }

    static final class Profile {
        final int storageState;
        final int sconfig;
        final int titleRes;
        final int summaryRes;
        final int iconRes;
        final Policy policy;

        Profile(int storageState, int sconfig, int titleRes, int summaryRes,
                int iconRes, Policy policy) {
            this.storageState = storageState;
            this.sconfig = sconfig;
            this.titleRes = titleRes;
            this.summaryRes = summaryRes;
            this.iconRes = iconRes;
            this.policy = policy;
        }
    }

    private static int[] ints(int... values) {
        return values;
    }

    private static Policy policy(String configName,
            int[] cpu4Trigger, int[] cpu4Clear, int[] cpu4Khz,
            int[] cpu7Trigger, int[] cpu7Clear, int[] cpu7Khz,
            int[] gpuTrigger, int[] gpuClear, int[] gpuState,
            int[] batteryTrigger, int[] tempStateTrigger,
            int controls, boolean definesVirtualSensor) {
        return new Policy(configName, cpu4Trigger, cpu4Clear, cpu4Khz,
                cpu7Trigger, cpu7Clear, cpu7Khz,
                gpuTrigger, gpuClear, gpuState,
                batteryTrigger, tempStateTrigger, controls, definesVirtualSensor);
    }

    private static Profile profile(int storageState, int sconfig, int titleRes,
            int summaryRes, int iconRes, Policy policy) {
        return new Profile(storageState, sconfig, titleRes, summaryRes, iconRes, policy);
    }

    private static final Profile[] GLOBAL_PROFILES = {
        profile(13, 0, R.string.thermal_normal, R.string.thermal_normal_summary,
                R.drawable.ic_thermal_default, policy("thermal-normal.conf",
                        ints(15, 41, 43, 45, 48), ints(14, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1056000, 710400),
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1401600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 40, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, true)),
        profile(2, 11, R.string.thermal_class0, R.string.thermal_class0_summary,
                R.drawable.ic_thermal_conservative, policy("thermal-class0.conf",
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(15, 37, 39, 41, 43, 45, 48), ints(14, 35, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(4, 8, R.string.thermal_calls, R.string.thermal_calls_summary,
                R.drawable.ic_thermal_dialer, policy("thermal-phone.conf",
                        ints(27, 48), ints(25, 45), ints(1286400, 710400),
                        ints(27, 48), ints(25, 45), ints(1305600, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(20, 60), ints(48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM | CONTROL_WIRELESS, false)),
        profile(3, 12, R.string.thermal_camera, R.string.thermal_camera_summary,
                R.drawable.ic_thermal_camera, policy("thermal-camera.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(5, 9, R.string.thermal_gaming, R.string.thermal_gaming_summary,
                R.drawable.ic_thermal_gaming, policy("thermal-tgame.conf",
                        ints(50), ints(47), ints(710400),
                        ints(50), ints(47), ints(844800),
                        ints(48), ints(45), ints(2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(8, 20, R.string.thermal_sustained_gaming, R.string.thermal_sustained_gaming_summary,
                R.drawable.ic_thermal_sustained_gaming, policy("thermal-mgame.conf",
                        ints(15, 48), ints(14, 45), ints(1766400, 710400),
                        ints(15, 48), ints(14, 45), ints(1862400, 844800),
                        ints(48), ints(45), ints(2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM | CONTROL_HOTPLUG, false)),
        profile(6, 21, R.string.thermal_video, R.string.thermal_video_summary,
                R.drawable.ic_thermal_streaming, policy("thermal-video.conf",
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(15, 37, 39, 41, 43, 45, 48), ints(14, 35, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(7, 19, R.string.thermal_navigation, R.string.thermal_navigation_summary,
                R.drawable.ic_thermal_browser, policy("thermal-navigation.conf",
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(15, 37, 39, 41, 43, 45, 48), ints(14, 35, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(1, 10, R.string.thermal_no_limits, R.string.thermal_no_limits_summary,
                R.drawable.ic_thermal_minimal_throttling, policy("thermal-nolimits.conf",
                        ints(51), ints(49), ints(710400),
                        ints(51), ints(49), ints(844800),
                        ints(48), ints(45), ints(1),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(55),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE, false)),
        profile(9, 30, R.string.thermal_per_normal, R.string.thermal_per_normal_summary,
                R.drawable.ic_thermal_balanced_performance, policy("thermal-per-normal.conf",
                        ints(39, 41, 43, 45, 48), ints(37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1056000, 710400),
                        ints(39, 41, 43, 45, 48), ints(37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 40, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, true)),
        profile(10, 41, R.string.thermal_per_class0, R.string.thermal_per_class0_summary,
                R.drawable.ic_thermal_conservative_performance, policy("thermal-per-class0.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(11, 49, R.string.thermal_per_navigation, R.string.thermal_per_navigation_summary,
                R.drawable.ic_thermal_navigation_performance, policy("thermal-per-navigation.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(12, 51, R.string.thermal_per_video, R.string.thermal_per_video_summary,
                R.drawable.ic_thermal_video_performance, policy("thermal-per-video.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false))
    };

    private static final Profile[] INDIA_PROFILES = {
        profile(13, 0, R.string.thermal_normal, R.string.thermal_normal_summary,
                R.drawable.ic_thermal_default, policy("thermal-india-normal.conf",
                        ints(15, 41, 43, 45, 48), ints(14, 39, 41, 43, 45), ints(2246400, 1766400, 1382400, 1056000, 710400),
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 38, 40, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, true)),
        profile(2, 11, R.string.thermal_class0, R.string.thermal_class0_summary,
                R.drawable.ic_thermal_conservative, policy("thermal-india-class0.conf",
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(15, 37, 39, 41, 43, 45, 48), ints(14, 35, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(4, 8, R.string.thermal_calls, R.string.thermal_calls_summary,
                R.drawable.ic_thermal_dialer, policy("thermal-phone.conf",
                        ints(27, 48), ints(25, 45), ints(1286400, 710400),
                        ints(27, 48), ints(25, 45), ints(1305600, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(20, 60), ints(48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM | CONTROL_WIRELESS, false)),
        profile(3, 12, R.string.thermal_camera, R.string.thermal_camera_summary,
                R.drawable.ic_thermal_camera, policy("thermal-camera.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(5, 9, R.string.thermal_gaming, R.string.thermal_gaming_summary,
                R.drawable.ic_thermal_gaming, policy("thermal-india-tgame.conf",
                        ints(50), ints(47), ints(710400),
                        ints(50), ints(47), ints(844800),
                        ints(48), ints(45), ints(2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(8, 20, R.string.thermal_sustained_gaming, R.string.thermal_sustained_gaming_summary,
                R.drawable.ic_thermal_sustained_gaming, policy("thermal-india-mgame.conf",
                        ints(15, 48), ints(14, 45), ints(1766400, 710400),
                        ints(15, 48), ints(14, 45), ints(1862400, 844800),
                        ints(48), ints(45), ints(2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM | CONTROL_HOTPLUG, false)),
        profile(6, 21, R.string.thermal_video, R.string.thermal_video_summary,
                R.drawable.ic_thermal_streaming, policy("thermal-india-video.conf",
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(15, 37, 39, 41, 43, 45, 48), ints(14, 35, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(7, 19, R.string.thermal_navigation, R.string.thermal_navigation_summary,
                R.drawable.ic_thermal_browser, policy("thermal-navigation.conf",
                        ints(15, 39, 41, 43, 45, 48), ints(14, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(15, 37, 39, 41, 43, 45, 48), ints(14, 35, 37, 39, 41, 43, 45), ints(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(1, 10, R.string.thermal_no_limits, R.string.thermal_no_limits_summary,
                R.drawable.ic_thermal_minimal_throttling, policy("thermal-nolimits.conf",
                        ints(51), ints(49), ints(710400),
                        ints(51), ints(49), ints(844800),
                        ints(48), ints(45), ints(1),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(55),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE, false)),
        profile(9, 30, R.string.thermal_per_normal, R.string.thermal_per_normal_summary,
                R.drawable.ic_thermal_balanced_performance, policy("thermal-india-per-normal.conf",
                        ints(39, 41, 43, 45, 48), ints(37, 39, 41, 43, 45), ints(2246400, 1766400, 1382400, 1056000, 710400),
                        ints(39, 41, 43, 45, 48), ints(37, 39, 41, 43, 45), ints(2457600, 1862400, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 38, 40, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, true)),
        profile(10, 41, R.string.thermal_per_class0, R.string.thermal_per_class0_summary,
                R.drawable.ic_thermal_conservative_performance, policy("thermal-india-per-class0.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(11, 49, R.string.thermal_per_navigation, R.string.thermal_per_navigation_summary,
                R.drawable.ic_thermal_navigation_performance, policy("thermal-per-navigation.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false)),
        profile(12, 51, R.string.thermal_per_video, R.string.thermal_per_video_summary,
                R.drawable.ic_thermal_video_performance, policy("thermal-india-per-video.conf",
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                        ints(37, 39, 41, 43, 45, 48), ints(35, 37, 39, 41, 43, 45), ints(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                        ints(41, 43, 45), ints(39, 41, 43), ints(0, 1, 2),
                        ints(37, 39, 41, 43, 45, 48, 60), ints(45, 48, 52),
                        CONTROL_BATTERY | CONTROL_TEMP_STATE | CONTROL_HOTPLUG | CONTROL_BOOST_LIMIT | CONTROL_LOW_BATTERY | CONTROL_HBM | CONTROL_BACKLIGHT | CONTROL_MODEM, false))
    };

    static Profile[] getProfiles(int region) {
        return region == REGION_INDIA ? INDIA_PROFILES : GLOBAL_PROFILES;
    }

    static Profile findByStorageState(int region, int state) {
        for (Profile profile : getProfiles(region)) {
            if (profile.storageState == state) return profile;
        }
        return null;
    }

    static Profile findBySconfig(int region, int sconfig) {
        for (Profile profile : getProfiles(region)) {
            if (profile.sconfig == sconfig) return profile;
        }
        return null;
    }

    private ThermalProfiles() {}
}
