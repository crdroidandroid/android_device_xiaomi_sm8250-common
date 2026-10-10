/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.thermal

import android.content.Context
import java.util.Locale
import org.lineageos.settings.R

class ThermalDetails(private val context: Context) {
    private fun text(id: Int, vararg args: Any) =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)

    private fun mhz(value: Double) =
        String.format(
            Locale.getDefault(),
            if (Math.rint(value) == value) "%.0f MHz" else "%.1f MHz",
            value,
        )

    private fun cpuMhz(value: Int) = String.format(Locale.getDefault(), "%.1f MHz", value / 1000f)

    private fun gpuMhz(state: Int) =
        doubleArrayOf(670.0, 587.0, 525.0, 490.0, 441.6, 400.0, 305.0).getOrElse(state) { 0.0 }

    fun cpu(p: ThermalProfiles.Policy) =
        curve(
            text(R.string.thermal_cpu_performance_cluster),
            p.cpu4Trigger,
            p.cpu4Clear,
            p.cpu4Khz,
        ) +
            "\n\n" +
            curve(text(R.string.thermal_cpu_prime_core), p.cpu7Trigger, p.cpu7Clear, p.cpu7Khz)

    private fun curve(label: String, trigger: IntArray, clear: IntArray, khz: IntArray): String =
        buildString {
            append(label)
            append('\n')
            if (trigger.isEmpty() || khz.isEmpty()) {
                append(text(R.string.thermal_no_profile_ceiling))
                return@buildString
            }
            append(text(R.string.thermal_below_no_cap, trigger[0]))
            append('\n')
            val min = khz.min()
            val max = khz.max()
            append(
                if (min == max) text(R.string.thermal_configured_ceiling, cpuMhz(max))
                else text(R.string.thermal_configured_range, cpuMhz(min), cpuMhz(max))
            )
            for (i in 0 until minOf(trigger.size, khz.size)) {
                append('\n')
                append(
                    if (i < clear.size)
                        text(
                            R.string.thermal_trigger_clear_cap,
                            trigger[i],
                            clear[i],
                            cpuMhz(khz[i]),
                        )
                    else text(R.string.thermal_trigger_cap, trigger[i], cpuMhz(khz[i]))
                )
            }
        }

    fun gpu(p: ThermalProfiles.Policy): String = buildString {
        if (p.gpuTrigger.isEmpty() || p.gpuState.isEmpty()) {
            append(text(R.string.thermal_no_gpu_ceiling))
            return@buildString
        }
        append(text(R.string.thermal_below_no_cap, p.gpuTrigger[0]))
        append('\n')
        val values = p.gpuState.map { gpuMhz(it) }
        val min = values.min()
        val max = values.max()
        append(
            if (min == max) text(R.string.thermal_configured_ceiling, mhz(max))
            else text(R.string.thermal_configured_range, mhz(min), mhz(max))
        )
        for (i in 0 until minOf(p.gpuTrigger.size, p.gpuState.size)) {
            append('\n')
            append(
                if (i < p.gpuClear.size)
                    text(
                        R.string.thermal_gpu_trigger_clear_cap,
                        p.gpuTrigger[i],
                        p.gpuClear[i],
                        p.gpuState[i],
                        mhz(gpuMhz(p.gpuState[i])),
                    )
                else
                    text(
                        R.string.thermal_gpu_trigger_cap,
                        p.gpuTrigger[i],
                        p.gpuState[i],
                        mhz(gpuMhz(p.gpuState[i])),
                    )
            )
        }
        append("\n\n")
        append(text(R.string.thermal_gpu_bin_note))
    }

    fun controls(p: ThermalProfiles.Policy): String {
        val lines = mutableListOf<String>()
        fun line(flag: Int, label: Int, value: String) {
            if (p.hasControl(flag)) lines += text(R.string.thermal_control_line, text(label), value)
        }
        fun range(flag: Int, label: Int, values: IntArray) {
            if (values.isNotEmpty())
                line(
                    flag,
                    label,
                    if (values.size == 1) text(R.string.thermal_temperature_value, values[0])
                    else text(R.string.thermal_temperature_range, values.first(), values.last()),
                )
        }
        range(ThermalProfiles.CONTROL_BATTERY, R.string.thermal_control_battery, p.batteryTrigger)
        range(
            ThermalProfiles.CONTROL_TEMP_STATE,
            R.string.thermal_control_state,
            p.tempStateTrigger,
        )
        line(
            ThermalProfiles.CONTROL_HOTPLUG,
            R.string.thermal_control_hotplug,
            text(R.string.thermal_temperature_value, 50),
        )
        line(
            ThermalProfiles.CONTROL_BOOST_LIMIT,
            R.string.thermal_control_boost,
            text(R.string.thermal_temperature_value, 55),
        )
        line(
            ThermalProfiles.CONTROL_LOW_BATTERY,
            R.string.thermal_control_low_battery,
            text(R.string.thermal_control_low_battery_value),
        )
        line(
            ThermalProfiles.CONTROL_HBM,
            R.string.thermal_control_hbm,
            text(R.string.thermal_temperature_value, 40),
        )
        line(
            ThermalProfiles.CONTROL_BACKLIGHT,
            R.string.thermal_control_backlight,
            text(R.string.thermal_temperature_range, 37, 49),
        )
        line(
            ThermalProfiles.CONTROL_MODEM,
            R.string.thermal_control_modem,
            text(R.string.thermal_temperature_value, 48),
        )
        line(
            ThermalProfiles.CONTROL_WIRELESS,
            R.string.thermal_control_wireless,
            text(R.string.thermal_control_wireless_value),
        )
        return if (lines.isEmpty()) text(R.string.thermal_control_none)
        else lines.joinToString("\n")
    }

    fun sensor(p: ThermalProfiles.Policy) =
        text(R.string.thermal_sensor_model) +
            "\n\n" +
            text(
                if (p.definesVirtualSensor) R.string.thermal_sensor_defined
                else R.string.thermal_sensor_referenced
            )
}
