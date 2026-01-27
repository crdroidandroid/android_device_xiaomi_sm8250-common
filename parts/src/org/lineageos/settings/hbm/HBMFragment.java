/*
* Copyright (C) 2016 The OmniROM Project
* Copyright (C) 2018-2021 crDroid Android Project
* Copyright (C) 2019-2022 Evolution X Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.lineageos.settings.hbm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.widget.SettingsBasePreferenceFragment;

import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.R;

public class HBMFragment extends SettingsBasePreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = HBMFragment.class.getSimpleName();

    public static final String HBM_SWITCH_KEY = "hbm";
    public static final String AUTO_HBM_SWITCH_KEY = "auto_hbm";
    public static final String AUTO_HBM_THRESHOLD_KEY = "auto_hbm_threshold";
    public static final String HBM_DISABLE_TIME_KEY = "hbm_disable_time";

    private static TwoStatePreference mHBMModeSwitch;
    private static TwoStatePreference mAutoHBMSwitch;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        setPreferencesFromResource(R.xml.hbm_settings, rootKey);

        // HBM
        mHBMModeSwitch = (TwoStatePreference) findPreference(HBM_SWITCH_KEY);
	    mHBMModeSwitch.setOnPreferenceChangeListener(new HBMModeSwitch(getContext()));

        // AutoHBM
        mAutoHBMSwitch = (TwoStatePreference) findPreference(AUTO_HBM_SWITCH_KEY);
        mAutoHBMSwitch.setOnPreferenceChangeListener(this);
        mAutoHBMSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean(HBMFragment.AUTO_HBM_SWITCH_KEY, false));
    }

    public static boolean isAUTOHBMEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(HBMFragment.AUTO_HBM_SWITCH_KEY, false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAutoHBMSwitch) {
            Boolean enabled = (Boolean) newValue;
            SharedPreferences.Editor prefChange = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            prefChange.putBoolean(AUTO_HBM_SWITCH_KEY, enabled).commit();
            FileUtils.enableService(getContext());
            return true;
           }

        return false;
    }
}
