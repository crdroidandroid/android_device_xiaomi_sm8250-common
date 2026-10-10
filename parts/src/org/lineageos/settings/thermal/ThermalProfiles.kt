/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.thermal

import org.lineageos.settings.R

object ThermalProfiles {
    const val REGION_GLOBAL = 0
    const val REGION_INDIA = 1
    const val CONTROL_BATTERY = 1 shl 0
    const val CONTROL_TEMP_STATE = 1 shl 1
    const val CONTROL_HOTPLUG = 1 shl 2
    const val CONTROL_BOOST_LIMIT = 1 shl 3
    const val CONTROL_LOW_BATTERY = 1 shl 4
    const val CONTROL_HBM = 1 shl 5
    const val CONTROL_BACKLIGHT = 1 shl 6
    const val CONTROL_MODEM = 1 shl 7
    const val CONTROL_WIRELESS = 1 shl 8

    data class Policy(
        val configName: String,
        val cpu4Trigger: IntArray,
        val cpu4Clear: IntArray,
        val cpu4Khz: IntArray,
        val cpu7Trigger: IntArray,
        val cpu7Clear: IntArray,
        val cpu7Khz: IntArray,
        val gpuTrigger: IntArray,
        val gpuClear: IntArray,
        val gpuState: IntArray,
        val batteryTrigger: IntArray,
        val tempStateTrigger: IntArray,
        val controls: Int,
        val definesVirtualSensor: Boolean,
    ) {
        fun hasControl(control: Int) = (controls and control) != 0

    }

    data class Profile(
        val storageState: Int,
        val sconfig: Int,
        val titleRes: Int,
        val summaryRes: Int,
        val iconRes: Int,
        val policy: Policy,
    )

    private val GLOBAL_PROFILES =
        arrayOf(
            Profile(
                13,
                0,
                R.string.thermal_normal,
                R.string.thermal_normal_summary,
                R.drawable.ic_thermal_default,
                Policy(
                    "thermal-normal.conf",
                    intArrayOf(15, 41, 43, 45, 48),
                    intArrayOf(14, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1056000, 710400),
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1401600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 40, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    true,
                ),
            ),
            Profile(
                2,
                11,
                R.string.thermal_class0,
                R.string.thermal_class0_summary,
                R.drawable.ic_thermal_conservative,
                Policy(
                    "thermal-class0.conf",
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(15, 37, 39, 41, 43, 45, 48),
                    intArrayOf(14, 35, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                4,
                8,
                R.string.thermal_calls,
                R.string.thermal_calls_summary,
                R.drawable.ic_thermal_dialer,
                Policy(
                    "thermal-phone.conf",
                    intArrayOf(27, 48),
                    intArrayOf(25, 45),
                    intArrayOf(1286400, 710400),
                    intArrayOf(27, 48),
                    intArrayOf(25, 45),
                    intArrayOf(1305600, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(20, 60),
                    intArrayOf(48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM or
                        CONTROL_WIRELESS,
                    false,
                ),
            ),
            Profile(
                3,
                12,
                R.string.thermal_camera,
                R.string.thermal_camera_summary,
                R.drawable.ic_thermal_camera,
                Policy(
                    "thermal-camera.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                5,
                9,
                R.string.thermal_gaming,
                R.string.thermal_gaming_summary,
                R.drawable.ic_thermal_gaming,
                Policy(
                    "thermal-tgame.conf",
                    intArrayOf(50),
                    intArrayOf(47),
                    intArrayOf(710400),
                    intArrayOf(50),
                    intArrayOf(47),
                    intArrayOf(844800),
                    intArrayOf(48),
                    intArrayOf(45),
                    intArrayOf(2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                8,
                20,
                R.string.thermal_sustained_gaming,
                R.string.thermal_sustained_gaming_summary,
                R.drawable.ic_thermal_sustained_gaming,
                Policy(
                    "thermal-mgame.conf",
                    intArrayOf(15, 48),
                    intArrayOf(14, 45),
                    intArrayOf(1766400, 710400),
                    intArrayOf(15, 48),
                    intArrayOf(14, 45),
                    intArrayOf(1862400, 844800),
                    intArrayOf(48),
                    intArrayOf(45),
                    intArrayOf(2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM or
                        CONTROL_HOTPLUG,
                    false,
                ),
            ),
            Profile(
                6,
                21,
                R.string.thermal_video,
                R.string.thermal_video_summary,
                R.drawable.ic_thermal_streaming,
                Policy(
                    "thermal-video.conf",
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(15, 37, 39, 41, 43, 45, 48),
                    intArrayOf(14, 35, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                7,
                19,
                R.string.thermal_navigation,
                R.string.thermal_navigation_summary,
                R.drawable.ic_thermal_browser,
                Policy(
                    "thermal-navigation.conf",
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(15, 37, 39, 41, 43, 45, 48),
                    intArrayOf(14, 35, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                1,
                10,
                R.string.thermal_no_limits,
                R.string.thermal_no_limits_summary,
                R.drawable.ic_thermal_minimal_throttling,
                Policy(
                    "thermal-nolimits.conf",
                    intArrayOf(51),
                    intArrayOf(49),
                    intArrayOf(710400),
                    intArrayOf(51),
                    intArrayOf(49),
                    intArrayOf(844800),
                    intArrayOf(48),
                    intArrayOf(45),
                    intArrayOf(1),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(55),
                    CONTROL_BATTERY or CONTROL_TEMP_STATE,
                    false,
                ),
            ),
            Profile(
                9,
                30,
                R.string.thermal_per_normal,
                R.string.thermal_per_normal_summary,
                R.drawable.ic_thermal_balanced_performance,
                Policy(
                    "thermal-per-normal.conf",
                    intArrayOf(39, 41, 43, 45, 48),
                    intArrayOf(37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1056000, 710400),
                    intArrayOf(39, 41, 43, 45, 48),
                    intArrayOf(37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 40, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    true,
                ),
            ),
            Profile(
                10,
                41,
                R.string.thermal_per_class0,
                R.string.thermal_per_class0_summary,
                R.drawable.ic_thermal_conservative_performance,
                Policy(
                    "thermal-per-class0.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                11,
                49,
                R.string.thermal_per_navigation,
                R.string.thermal_per_navigation_summary,
                R.drawable.ic_thermal_navigation_performance,
                Policy(
                    "thermal-per-navigation.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                12,
                51,
                R.string.thermal_per_video,
                R.string.thermal_per_video_summary,
                R.drawable.ic_thermal_video_performance,
                Policy(
                    "thermal-per-video.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
        )

    private val INDIA_PROFILES =
        arrayOf(
            Profile(
                13,
                0,
                R.string.thermal_normal,
                R.string.thermal_normal_summary,
                R.drawable.ic_thermal_default,
                Policy(
                    "thermal-india-normal.conf",
                    intArrayOf(15, 41, 43, 45, 48),
                    intArrayOf(14, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1382400, 1056000, 710400),
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 38, 40, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    true,
                ),
            ),
            Profile(
                2,
                11,
                R.string.thermal_class0,
                R.string.thermal_class0_summary,
                R.drawable.ic_thermal_conservative,
                Policy(
                    "thermal-india-class0.conf",
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(15, 37, 39, 41, 43, 45, 48),
                    intArrayOf(14, 35, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                4,
                8,
                R.string.thermal_calls,
                R.string.thermal_calls_summary,
                R.drawable.ic_thermal_dialer,
                Policy(
                    "thermal-phone.conf",
                    intArrayOf(27, 48),
                    intArrayOf(25, 45),
                    intArrayOf(1286400, 710400),
                    intArrayOf(27, 48),
                    intArrayOf(25, 45),
                    intArrayOf(1305600, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(20, 60),
                    intArrayOf(48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM or
                        CONTROL_WIRELESS,
                    false,
                ),
            ),
            Profile(
                3,
                12,
                R.string.thermal_camera,
                R.string.thermal_camera_summary,
                R.drawable.ic_thermal_camera,
                Policy(
                    "thermal-camera.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                5,
                9,
                R.string.thermal_gaming,
                R.string.thermal_gaming_summary,
                R.drawable.ic_thermal_gaming,
                Policy(
                    "thermal-india-tgame.conf",
                    intArrayOf(50),
                    intArrayOf(47),
                    intArrayOf(710400),
                    intArrayOf(50),
                    intArrayOf(47),
                    intArrayOf(844800),
                    intArrayOf(48),
                    intArrayOf(45),
                    intArrayOf(2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                8,
                20,
                R.string.thermal_sustained_gaming,
                R.string.thermal_sustained_gaming_summary,
                R.drawable.ic_thermal_sustained_gaming,
                Policy(
                    "thermal-india-mgame.conf",
                    intArrayOf(15, 48),
                    intArrayOf(14, 45),
                    intArrayOf(1766400, 710400),
                    intArrayOf(15, 48),
                    intArrayOf(14, 45),
                    intArrayOf(1862400, 844800),
                    intArrayOf(48),
                    intArrayOf(45),
                    intArrayOf(2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM or
                        CONTROL_HOTPLUG,
                    false,
                ),
            ),
            Profile(
                6,
                21,
                R.string.thermal_video,
                R.string.thermal_video_summary,
                R.drawable.ic_thermal_streaming,
                Policy(
                    "thermal-india-video.conf",
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(15, 37, 39, 41, 43, 45, 48),
                    intArrayOf(14, 35, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                7,
                19,
                R.string.thermal_navigation,
                R.string.thermal_navigation_summary,
                R.drawable.ic_thermal_browser,
                Policy(
                    "thermal-navigation.conf",
                    intArrayOf(15, 39, 41, 43, 45, 48),
                    intArrayOf(14, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(15, 37, 39, 41, 43, 45, 48),
                    intArrayOf(14, 35, 37, 39, 41, 43, 45),
                    intArrayOf(2745600, 2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                1,
                10,
                R.string.thermal_no_limits,
                R.string.thermal_no_limits_summary,
                R.drawable.ic_thermal_minimal_throttling,
                Policy(
                    "thermal-nolimits.conf",
                    intArrayOf(51),
                    intArrayOf(49),
                    intArrayOf(710400),
                    intArrayOf(51),
                    intArrayOf(49),
                    intArrayOf(844800),
                    intArrayOf(48),
                    intArrayOf(45),
                    intArrayOf(1),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(55),
                    CONTROL_BATTERY or CONTROL_TEMP_STATE,
                    false,
                ),
            ),
            Profile(
                9,
                30,
                R.string.thermal_per_normal,
                R.string.thermal_per_normal_summary,
                R.drawable.ic_thermal_balanced_performance,
                Policy(
                    "thermal-india-per-normal.conf",
                    intArrayOf(39, 41, 43, 45, 48),
                    intArrayOf(37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1382400, 1056000, 710400),
                    intArrayOf(39, 41, 43, 45, 48),
                    intArrayOf(37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 38, 40, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    true,
                ),
            ),
            Profile(
                10,
                41,
                R.string.thermal_per_class0,
                R.string.thermal_per_class0_summary,
                R.drawable.ic_thermal_conservative_performance,
                Policy(
                    "thermal-india-per-class0.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                11,
                49,
                R.string.thermal_per_navigation,
                R.string.thermal_per_navigation_summary,
                R.drawable.ic_thermal_navigation_performance,
                Policy(
                    "thermal-per-navigation.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
            Profile(
                12,
                51,
                R.string.thermal_per_video,
                R.string.thermal_per_video_summary,
                R.drawable.ic_thermal_video_performance,
                Policy(
                    "thermal-india-per-video.conf",
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2246400, 1766400, 1478400, 1286400, 1056000, 710400),
                    intArrayOf(37, 39, 41, 43, 45, 48),
                    intArrayOf(35, 37, 39, 41, 43, 45),
                    intArrayOf(2457600, 1862400, 1401600, 1305600, 1075200, 844800),
                    intArrayOf(41, 43, 45),
                    intArrayOf(39, 41, 43),
                    intArrayOf(0, 1, 2),
                    intArrayOf(37, 39, 41, 43, 45, 48, 60),
                    intArrayOf(45, 48, 52),
                    CONTROL_BATTERY or
                        CONTROL_TEMP_STATE or
                        CONTROL_HOTPLUG or
                        CONTROL_BOOST_LIMIT or
                        CONTROL_LOW_BATTERY or
                        CONTROL_HBM or
                        CONTROL_BACKLIGHT or
                        CONTROL_MODEM,
                    false,
                ),
            ),
        )

    fun getProfiles(region: Int) = if (region == REGION_INDIA) INDIA_PROFILES else GLOBAL_PROFILES

    fun findByStorageState(region: Int, state: Int) =
        getProfiles(region).firstOrNull { it.storageState == state }

    fun findBySconfig(region: Int, sconfig: Int) =
        getProfiles(region).firstOrNull { it.sconfig == sconfig }
}
