package org.lineageos.settings.dcdimming;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreferenceCompat;

import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.hbm.AutoHBMService;

public class DcDimmingSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";
    private static final String DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable";
    private static final String HBM_ENABLE_KEY = "hbm";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String AUTO_HBM_ENABLE_KEY = "auto_hbm";
    private static final String ACTION_DC_CHANGED = "org.lineageos.settings.device.DC_CHANGED";
    private static final String ACTION_HBM_CHANGED = "org.lineageos.settings.device.HBM_CHANGED";
    private static final String ACTION_AUTO_HBM_CHANGED = "org.lineageos.settings.device.AUTO_HBM_CHANGED";

    private Context mContext;
    private SwitchPreferenceCompat mDcDimmingPreference;
    private SharedPreferences mSharedPrefs;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_DC_CHANGED.equals(intent.getAction())) {
                mDcDimmingPreference.setChecked(mSharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false));
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mContext = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        addPreferencesFromResource(R.xml.dcdimming_settings);
        initializeDcDimmingPreference();
        registerReceiver();
    }

    private void initializeDcDimmingPreference() {
        mDcDimmingPreference = findPreference(DC_DIMMING_ENABLE_KEY);
        boolean nodeExists = FileUtils.fileExists(DC_DIMMING_NODE);
        mDcDimmingPreference.setEnabled(nodeExists);
        if (nodeExists) {
            mDcDimmingPreference.setOnPreferenceChangeListener(this);
        } else {
            mDcDimmingPreference.setSummary(R.string.dc_dimming_enable_summary_not_supported);
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_DC_CHANGED);
        mContext.registerReceiver(mReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (DC_DIMMING_ENABLE_KEY.equals(preference.getKey())) {
            boolean newState = (Boolean) newValue;
            handleDcDimmingChange(newState);
        }
        return true;
    }

    private void handleDcDimmingChange(boolean newState) {
        FileUtils.writeLine(DC_DIMMING_NODE, newState ? "1" : "0");
        if (newState) {
            disableHBMFeatures();
        }
        updatePreferences(newState);
    }

    private void disableHBMFeatures() {
        if (FileUtils.getFileValueAsBoolean(HBM_NODE, false)) {
            FileUtils.writeLine(HBM_NODE, "0");
            updateHBMState(false);
        }
        if (mSharedPrefs.getBoolean(AUTO_HBM_ENABLE_KEY, false)) {
            disableAutoHBM();
        }
    }

    private void updateHBMState(boolean state) {
        mSharedPrefs.edit().putBoolean(HBM_ENABLE_KEY, state).apply();
        broadcastStateChange(ACTION_HBM_CHANGED, state);
    }

    private void disableAutoHBM() {
        mSharedPrefs.edit().putBoolean(AUTO_HBM_ENABLE_KEY, false).apply();
        mContext.stopService(new Intent(mContext, AutoHBMService.class));
        broadcastStateChange(ACTION_AUTO_HBM_CHANGED, false);
    }

    private void updatePreferences(boolean newState) {
        mSharedPrefs.edit().putBoolean(DC_DIMMING_ENABLE_KEY, newState).apply();
        broadcastStateChange(ACTION_DC_CHANGED, newState);
    }

    private void broadcastStateChange(String action, boolean state) {
        Intent intent = new Intent(action);
        intent.putExtra("state", state);
        mContext.sendBroadcast(intent);
    }
}