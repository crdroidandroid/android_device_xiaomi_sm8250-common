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

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;
import com.android.settingslib.widget.SettingsBasePreferenceFragment;
import org.lineageos.settings.R;

public class ChargeSettingsFragment extends SettingsBasePreferenceFragment 
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_BYPASS_CHARGE = "bypass_charge";

    private ChargeUtils chargeUtils;
    private SwitchPreferenceCompat bypassChargePreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.charge_settings, rootKey);

        chargeUtils = new ChargeUtils(requireActivity());
        bypassChargePreference = findPreference(KEY_BYPASS_CHARGE);

        boolean bypassChargeSupported = chargeUtils.isBypassChargeSupported();

        if (bypassChargePreference != null) {
            bypassChargePreference.setEnabled(bypassChargeSupported);
            if (bypassChargeSupported) {
                bypassChargePreference.setChecked(chargeUtils.isBypassChargeEnabled());
                bypassChargePreference.setOnPreferenceChangeListener(this);
            } else {
                bypassChargePreference.setSummary(R.string.charge_bypass_unavailable);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_BYPASS_CHARGE.equals(preference.getKey())) {
            boolean bypassValue = (Boolean) newValue;
            if (bypassValue) {
                ChargeUtils.SafetyCheckResult safetyCheck = chargeUtils.performSafetyChecks();

                if (!safetyCheck.isSafe()) {
                    new AlertDialog.Builder(requireActivity())
                            .setTitle(R.string.charge_bypass_title)
                            .setMessage(getString(R.string.charge_bypass_safety_failed, 
                                    safetyCheck.getReason()))
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return false;
                }

                new AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.charge_bypass_title)
                        .setMessage(R.string.charge_bypass_warning)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            chargeUtils.enableBypassCharge(true);
                            if (bypassChargePreference != null) {
                                bypassChargePreference.setChecked(true);
                            }
                            try {
                                BypassChargeTileService.updateTile(requireActivity());
                            } catch (Exception e) {
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            if (bypassChargePreference != null) {
                                bypassChargePreference.setChecked(false);
                            }
                        })
                        .show();
                return false;
            } else {
                chargeUtils.enableBypassCharge(false);
                try {
                    BypassChargeTileService.updateTile(requireActivity());
                } catch (Exception e) {
                }
                return true;
            }
        }
        return false;
    }
}
