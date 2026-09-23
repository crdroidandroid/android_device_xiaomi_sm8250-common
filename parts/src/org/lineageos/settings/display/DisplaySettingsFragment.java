/*
 * Copyright (C) 2018 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.settings.display;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class DisplaySettingsFragment extends SettingsBasePreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private SwitchPreferenceCompat mDcDimmingPreference;
    private SwitchPreferenceCompat mHbmPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.display_settings);

        mDcDimmingPreference = findPreference(DisplayNodes.getDcDimmingEnableKey());
        mHbmPreference = findPreference(DisplayNodes.getHbmEnableKey());

        mDcDimmingPreference.setPersistent(false);
        mHbmPreference.setPersistent(false);
        mDcDimmingPreference.setOnPreferenceChangeListener(this);
        mHbmPreference.setOnPreferenceChangeListener(this);

        refreshState();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshState();
    }

    private void refreshState() {
        boolean dcSupported = DisplayUtils.isDcDimmingSupported();
        mDcDimmingPreference.setEnabled(dcSupported);
        mDcDimmingPreference.setChecked(dcSupported && DisplayUtils.isDcDimmingEnabled());
        if (!dcSupported) {
            mDcDimmingPreference.setSummary(R.string.dc_dimming_enable_summary_not_supported);
        } else {
            mDcDimmingPreference.setSummary(R.string.dc_dimming_enable_summary);
        }

        boolean hbmSupported = DisplayUtils.isHbmSupported();
        mHbmPreference.setEnabled(hbmSupported);
        mHbmPreference.setChecked(hbmSupported && DisplayUtils.isHbmEnabled());
        if (!hbmSupported) {
            mHbmPreference.setSummary(R.string.hbm_enable_summary_not_supported);
        } else {
            mHbmPreference.setSummary(R.string.hbm_mode_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        boolean applied;
        if (DisplayNodes.getDcDimmingEnableKey().equals(preference.getKey())) {
            applied = DisplayUtils.setDcDimming(requireContext(), enabled);
        } else if (DisplayNodes.getHbmEnableKey().equals(preference.getKey())) {
            applied = DisplayUtils.setHbm(requireContext(), enabled);
        } else {
            return false;
        }

        if (!applied) {
            Toast.makeText(requireContext(), R.string.parts_apply_failed, Toast.LENGTH_SHORT).show();
            refreshState();
        }
        return applied;
    }
}
