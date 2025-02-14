package org.lineageos.settings.hbm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragment;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.R;

public class HBMFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "HBMFragment";

    // Constants for preference keys and system nodes
    private static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";
    private static final String DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable";
    private static final String HBM_ENABLE_KEY = "hbm";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String AUTO_HBM_ENABLE_KEY = "auto_hbm";
    private static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";
    
    // Intent actions
    private static final String ACTION_HBM_CHANGED = "org.lineageos.settings.device.HBM_CHANGED";
    private static final String ACTION_AUTO_HBM_CHANGED = "org.lineageos.settings.device.AUTO_HBM_CHANGED";
    private static final String ACTION_DC_CHANGED = "org.lineageos.settings.device.DC_CHANGED";

    private TwoStatePreference mHBMModeSwitch;
    private TwoStatePreference mAutoHBMSwitch;
    private SharedPreferences mSharedPrefs;
    private Context mContext;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case ACTION_HBM_CHANGED:
                    updateHBMState();
                    break;
                case ACTION_AUTO_HBM_CHANGED:
                    updateAutoHBMState();
                    break;
            }
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mContext = getContext();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        addPreferencesFromResource(R.xml.hbm_settings);
        initializeHBMPreferences();
        registerReceiver();
    }

    private void initializeHBMPreferences() {
        mHBMModeSwitch = findPreference(HBM_ENABLE_KEY);
        mAutoHBMSwitch = findPreference(AUTO_HBM_ENABLE_KEY);
        boolean hbmNodeExists = FileUtils.fileExists(HBM_NODE);
        
        if (hbmNodeExists) {
            mHBMModeSwitch.setOnPreferenceChangeListener(this);
            mAutoHBMSwitch.setOnPreferenceChangeListener(this);
        } else {
            mHBMModeSwitch.setSummary(R.string.hbm_enable_summary_not_supported);
            mHBMModeSwitch.setEnabled(false);
            mAutoHBMSwitch.setSummary(R.string.hbm_enable_summary_not_supported);
            mAutoHBMSwitch.setEnabled(false);
        }
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DC_CHANGED);
        filter.addAction(ACTION_HBM_CHANGED);
        filter.addAction(ACTION_AUTO_HBM_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        mContext.unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    public static boolean isAUTOHBMEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(AUTO_HBM_ENABLE_KEY, false);
    }

    private void updateHBMState() {
        boolean newState = mSharedPrefs.getBoolean(HBM_ENABLE_KEY, false);
        mHBMModeSwitch.setChecked(newState);
    }

    private void updateAutoHBMState() {
        boolean newState = mSharedPrefs.getBoolean(AUTO_HBM_ENABLE_KEY, false);
        mAutoHBMSwitch.setChecked(newState);
    }

    private void handleHBMModeChange(boolean newState) {
        FileUtils.writeLine(HBM_NODE, newState ? "1" : "0");
        SharedPreferences.Editor editor = mSharedPrefs.edit();

        if (newState) {
            disableAUTOHBMIfEnabled(editor);
            disableDCDimmingIfEnabled(editor);
            updateBrightnessSettings();
        }

        editor.putBoolean(HBM_ENABLE_KEY, newState).apply();
        broadcastStateChange(ACTION_HBM_CHANGED, newState);
    }

    private void handleAutoHBMChange(boolean newState) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        
        if (newState) {
            disableHBMIfEnabled(editor);
            disableDCDimmingIfEnabled(editor);
        }

        editor.putBoolean(AUTO_HBM_ENABLE_KEY, newState).apply();
        broadcastStateChange(ACTION_AUTO_HBM_CHANGED, newState);
        FileUtils.enableService(mContext);
    }

    private void disableHBMIfEnabled(SharedPreferences.Editor editor){
        if (mSharedPrefs.getBoolean(HBM_ENABLE_KEY, false)) {
            FileUtils.writeLine(HBM_NODE, "0");
            broadcastStateChange(ACTION_HBM_CHANGED, false);
            editor.putBoolean(HBM_ENABLE_KEY, false);
        }  
    }

    private void disableDCDimmingIfEnabled(SharedPreferences.Editor editor){
        if (mSharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false)) {
            broadcastStateChange(ACTION_DC_CHANGED, false);
            editor.putBoolean(DC_DIMMING_ENABLE_KEY, false);
        }  
    }

    private void disableAUTOHBMIfEnabled(SharedPreferences.Editor editor){
        if (mSharedPrefs.getBoolean(AUTO_HBM_ENABLE_KEY, false)) {

            broadcastStateChange(ACTION_AUTO_HBM_CHANGED, false);
            editor.putBoolean(AUTO_HBM_ENABLE_KEY, false);
        }
    }

    private void updateBrightnessSettings() {
        FileUtils.writeLine(BACKLIGHT_NODE, "2047");
        Settings.System.putInt(mContext.getContentResolver(), 
                Settings.System.SCREEN_BRIGHTNESS, 255);
    }

    private void broadcastStateChange(String action, boolean state) {
        Intent intent = new Intent(action);
        intent.putExtra("state", state);
        mContext.sendBroadcast(intent);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean newState = (Boolean) newValue;

        if (preference == mHBMModeSwitch) {
            handleHBMModeChange(newState);
            return true;
        }
        
        if (preference == mAutoHBMSwitch) {
            handleAutoHBMChange(newState);
            return true;
        }

        return false;
    }
}