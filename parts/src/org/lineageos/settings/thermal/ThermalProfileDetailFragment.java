/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.thermal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.lineageos.settings.R;

import java.util.Locale;

public class ThermalProfileDetailFragment extends Fragment {
    private static final String ARG_STATE = "storage_state";

    static ThermalProfileDetailFragment newInstance(int state) {
        ThermalProfileDetailFragment fragment = new ThermalProfileDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STATE, state);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.thermal_profile_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int state = getArguments() == null ? -1 : getArguments().getInt(ARG_STATE, -1);
        int region = ThermalUtils.getSelectedRegion();
        ThermalProfiles.Profile profile = ThermalProfiles.findByStorageState(region, state);
        if (profile == null) return;

        requireActivity().setTitle(profile.titleRes);
        ((TextView) view.findViewById(R.id.thermal_detail_summary)).setText(profile.summaryRes);
        String regionName = getString(region == ThermalProfiles.REGION_INDIA
                ? R.string.thermal_region_india : R.string.thermal_region_global);
        ((TextView) view.findViewById(R.id.thermal_detail_meta)).setText(
                getString(R.string.thermal_detail_meta,
                        regionName, profile.sconfig, profile.policy.configName));
        ((TextView) view.findViewById(R.id.thermal_detail_cpu)).setText(formatCpu(profile.policy));
        ((TextView) view.findViewById(R.id.thermal_detail_gpu)).setText(formatGpu(profile.policy));
        ((TextView) view.findViewById(R.id.thermal_detail_controls))
                .setText(formatControls(profile.policy));
        ((TextView) view.findViewById(R.id.thermal_detail_sensor))
                .setText(formatSensor(profile.policy));
    }

    private String formatCpu(ThermalProfiles.Policy p) {
        StringBuilder out = new StringBuilder();
        appendCpuCurve(out, getString(R.string.thermal_cpu_performance_cluster),
                p.cpu4Trigger, p.cpu4Clear, p.cpu4Khz);
        out.append("\n\n");
        appendCpuCurve(out, getString(R.string.thermal_cpu_prime_core),
                p.cpu7Trigger, p.cpu7Clear, p.cpu7Khz);
        return out.toString();
    }

    private void appendCpuCurve(StringBuilder out, String label,
            int[] trigger, int[] clear, int[] khz) {
        out.append(label).append('\n');
        if (trigger.length == 0 || khz.length == 0) {
            out.append(getString(R.string.thermal_no_profile_ceiling));
            return;
        }
        int min = khz[0], max = khz[0];
        for (int value : khz) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        out.append(getString(R.string.thermal_below_no_cap, trigger[0])).append('\n');
        if (min == max) {
            out.append(getString(R.string.thermal_configured_ceiling, formatMhz(max)))
                    .append('\n');
        } else {
            out.append(getString(R.string.thermal_configured_range,
                    formatMhz(min), formatMhz(max))).append('\n');
        }
        for (int i = 0; i < trigger.length && i < khz.length; i++) {
            if (i < clear.length) {
                out.append(getString(R.string.thermal_trigger_clear_cap,
                        trigger[i], clear[i], formatMhz(khz[i])));
            } else {
                out.append(getString(R.string.thermal_trigger_cap,
                        trigger[i], formatMhz(khz[i])));
            }
            if (i + 1 < trigger.length && i + 1 < khz.length) out.append('\n');
        }
    }

    private String formatGpu(ThermalProfiles.Policy p) {
        if (p.gpuTrigger.length == 0 || p.gpuState.length == 0) {
            return getString(R.string.thermal_no_gpu_ceiling);
        }
        double min = gpuMhzValue(p.gpuState[0]), max = min;
        for (int state : p.gpuState) {
            double mhz = gpuMhzValue(state);
            min = Math.min(min, mhz);
            max = Math.max(max, mhz);
        }
        StringBuilder out = new StringBuilder();
        out.append(getString(R.string.thermal_below_no_cap, p.gpuTrigger[0])).append('\n');
        if (Double.compare(min, max) == 0) {
            out.append(getString(R.string.thermal_configured_ceiling,
                    formatGpuMhzValue(max))).append('\n');
        } else {
            out.append(getString(R.string.thermal_configured_range,
                    formatGpuMhzValue(min), formatGpuMhzValue(max))).append('\n');
        }
        for (int i = 0; i < p.gpuTrigger.length && i < p.gpuState.length; i++) {
            if (i < p.gpuClear.length) {
                out.append(getString(R.string.thermal_gpu_trigger_clear_cap,
                        p.gpuTrigger[i], p.gpuClear[i], p.gpuState[i],
                        formatGpuMhz(p.gpuState[i])));
            } else {
                out.append(getString(R.string.thermal_gpu_trigger_cap,
                        p.gpuTrigger[i], p.gpuState[i], formatGpuMhz(p.gpuState[i])));
            }
            if (i + 1 < p.gpuTrigger.length && i + 1 < p.gpuState.length) out.append('\n');
        }
        out.append("\n\n").append(getString(R.string.thermal_gpu_bin_note));
        return out.toString();
    }

    private String formatControls(ThermalProfiles.Policy p) {
        StringBuilder out = new StringBuilder();
        if (p.hasControl(ThermalProfiles.CONTROL_BATTERY)) {
            appendRange(out, getString(R.string.thermal_control_battery), p.batteryTrigger);
        }
        if (p.hasControl(ThermalProfiles.CONTROL_TEMP_STATE)) {
            appendRange(out, getString(R.string.thermal_control_state), p.tempStateTrigger);
        }
        if (p.hasControl(ThermalProfiles.CONTROL_HOTPLUG)) {
            appendLine(out, getString(R.string.thermal_control_hotplug),
                    getString(R.string.thermal_temperature_value, 50));
        }
        if (p.hasControl(ThermalProfiles.CONTROL_BOOST_LIMIT)) {
            appendLine(out, getString(R.string.thermal_control_boost),
                    getString(R.string.thermal_temperature_value, 55));
        }
        if (p.hasControl(ThermalProfiles.CONTROL_LOW_BATTERY)) {
            appendLine(out, getString(R.string.thermal_control_low_battery),
                    getString(R.string.thermal_control_low_battery_value));
        }
        if (p.hasControl(ThermalProfiles.CONTROL_HBM)) {
            appendLine(out, getString(R.string.thermal_control_hbm),
                    getString(R.string.thermal_temperature_value, 40));
        }
        if (p.hasControl(ThermalProfiles.CONTROL_BACKLIGHT)) {
            appendLine(out, getString(R.string.thermal_control_backlight),
                    getString(R.string.thermal_temperature_range, 37, 49));
        }
        if (p.hasControl(ThermalProfiles.CONTROL_MODEM)) {
            appendLine(out, getString(R.string.thermal_control_modem),
                    getString(R.string.thermal_temperature_value, 48));
        }
        if (p.hasControl(ThermalProfiles.CONTROL_WIRELESS)) {
            appendLine(out, getString(R.string.thermal_control_wireless),
                    getString(R.string.thermal_control_wireless_value));
        }
        return out.length() == 0 ? getString(R.string.thermal_control_none) : out.toString();
    }

    private void appendRange(StringBuilder out, String label, int[] values) {
        if (values.length == 0) return;
        String value = values.length == 1
                ? getString(R.string.thermal_temperature_value, values[0])
                : getString(R.string.thermal_temperature_range,
                        values[0], values[values.length - 1]);
        appendLine(out, label, value);
    }

    private void appendLine(StringBuilder out, String label, String value) {
        if (out.length() > 0) out.append('\n');
        out.append(getString(R.string.thermal_control_line, label, value));
    }

    private String formatSensor(ThermalProfiles.Policy p) {
        return getString(R.string.thermal_sensor_model) + "\n\n"
                + getString(p.definesVirtualSensor
                        ? R.string.thermal_sensor_defined
                        : R.string.thermal_sensor_referenced);
    }

    private static String formatMhz(int khz) {
        return String.format(Locale.getDefault(), "%.1f MHz", khz / 1000f);
    }

    private static double gpuMhzValue(int state) {
        switch (state) {
            case 0: return 670.0;
            case 1: return 587.0;
            case 2: return 525.0;
            case 3: return 490.0;
            case 4: return 441.6;
            case 5: return 400.0;
            case 6: return 305.0;
            default: return 0.0;
        }
    }

    private static String formatGpuMhz(int state) {
        return formatGpuMhzValue(gpuMhzValue(state));
    }

    private static String formatGpuMhzValue(double mhz) {
        return Math.rint(mhz) == mhz
                ? String.format(Locale.getDefault(), "%.0f MHz", mhz)
                : String.format(Locale.getDefault(), "%.1f MHz", mhz);
    }

}
