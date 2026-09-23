/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.touchsampling;

import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.R;

public class TouchSamplingSettingsFragment extends SettingsBasePreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String HTSR_ENABLE_KEY = "htsr_enable";
    private SwitchPreferenceCompat mPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.htsr_settings);
        mPreference = findPreference(HTSR_ENABLE_KEY);
        mPreference.setPersistent(false);
        mPreference.setEnabled(TouchSamplingUtils.isSupported());
        mPreference.setChecked(TouchSamplingUtils.isEnabled(requireContext()));
        mPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean supported = TouchSamplingUtils.isSupported();
        mPreference.setEnabled(supported);
        mPreference.setChecked(supported && TouchSamplingUtils.isEnabled(requireContext()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!HTSR_ENABLE_KEY.equals(preference.getKey())) return false;
        boolean applied = TouchSamplingUtils.setEnabled(requireContext(), (Boolean) newValue);
        if (!applied) {
            Toast.makeText(requireContext(), R.string.parts_apply_failed, Toast.LENGTH_SHORT).show();
        }
        return applied;
    }
}
