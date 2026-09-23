/**
 * Copyright (C) 2020 The LineageOS Project
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
package org.lineageos.settings.thermal;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import com.android.settingslib.widget.SliderPreference;

import org.lineageos.settings.R;

/** Edits the existing package-specific four-value touch profile. */
public class TouchSettingsFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private SharedPreferences mPrefs;
    private String mPackageName;
    private MainSwitchPreference mGameMode;
    private SliderPreference mResponse, mSensitivity, mResistance;
    private final int[] mValues = new int[4];

    @Override
    public void onCreatePreferences(Bundle state, String rootKey) {
        addPreferencesFromResource(R.xml.touch_settings);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        mPackageName = requireArguments().getString("packageName", "");
        mGameMode = findPreference(Constants.PREF_TOUCH_GAME_MODE);
        mResponse = findPreference(Constants.PREF_TOUCH_RESPONSE);
        mSensitivity = findPreference(Constants.PREF_TOUCH_SENSITIVITY);
        mResistance = findPreference(Constants.PREF_TOUCH_RESISTANT);
        for (Preference pref : new Preference[]{mGameMode, mResponse, mSensitivity, mResistance}) {
            pref.setPersistent(false);
            pref.setOnPreferenceChangeListener(this);
        }
        mResponse.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
        mSensitivity.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
        mResistance.setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
        mGameMode.setSummary(requireArguments().getString("appName", mPackageName));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.touch_control_title);
        String[] saved = mPrefs.getString(mPackageName, "0,0,0,0").split(",", -1);
        for (int i = 0; i < mValues.length; i++) {
            int value = 0;
            try {
                if (saved.length == 4) value = Integer.parseInt(saved[i]);
            } catch (NumberFormatException ignored) { }
            mValues[i] = Math.max(0, Math.min(i == Constants.TOUCH_GAME_MODE ? 1
                    : getResources().getInteger(R.integer.smoothness_max), value));
        }
        mGameMode.setChecked(mValues[Constants.TOUCH_GAME_MODE] == 1);
        mResponse.setValue(mValues[Constants.TOUCH_RESPONSE]);
        mSensitivity.setValue(mValues[Constants.TOUCH_SENSITIVITY]);
        mResistance.setValue(mValues[Constants.TOUCH_RESISTANT]);
        mGameMode.setEnabled(!mPackageName.isEmpty());
        updateEnabled();
    }

    private void updateEnabled() {
        boolean enabled = !mPackageName.isEmpty() && mValues[Constants.TOUCH_GAME_MODE] == 1;
        mResponse.setEnabled(enabled);
        mSensitivity.setEnabled(enabled);
        mResistance.setEnabled(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value) {
        if (mPackageName.isEmpty()) return false;
        if (pref == mGameMode) mValues[Constants.TOUCH_GAME_MODE] = (Boolean) value ? 1 : 0;
        else if (pref == mResponse) mValues[Constants.TOUCH_RESPONSE] = (Integer) value;
        else if (pref == mSensitivity) mValues[Constants.TOUCH_SENSITIVITY] = (Integer) value;
        else if (pref == mResistance) mValues[Constants.TOUCH_RESISTANT] = (Integer) value;
        else return false;
        mPrefs.edit().putString(mPackageName, mValues[0] + "," + mValues[1] + ","
                + mValues[2] + "," + mValues[3]).apply();
        ThermalUtils.startService(requireContext());
        updateEnabled();
        return true;
    }
}
