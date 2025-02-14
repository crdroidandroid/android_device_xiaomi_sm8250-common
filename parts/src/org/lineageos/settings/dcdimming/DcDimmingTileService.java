package org.lineageos.settings.dcdimming;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;
import org.lineageos.settings.hbm.AutoHBMService;

public class DcDimmingTileService extends TileService {
    private static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";
    private static final String DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable";
    private static final String HBM_ENABLE_KEY = "hbm";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String AUTO_HBM_ENABLE_KEY = "auto_hbm";
    
    private static final String ACTION_DC_CHANGED = "org.lineageos.settings.device.DC_CHANGED";
    private static final String ACTION_HBM_CHANGED = "org.lineageos.settings.device.HBM_CHANGED";
    private static final String ACTION_AUTO_HBM_CHANGED = "org.lineageos.settings.device.AUTO_HBM_CHANGED";

    private SharedPreferences mSharedPrefs;
    
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_DC_CHANGED.equals(intent.getAction())) {
                updateUI(intent.getBooleanExtra("state", false));
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        registerReceiver(mReceiver, new IntentFilter(ACTION_DC_CHANGED));
        updateUI(mSharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false));
    }

    @Override
    public void onStopListening() {
        unregisterReceiver(mReceiver);
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean newState = !mSharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false);
        handleDcDimmingChange(newState);
    }

    private void updateUI(boolean newState) {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(newState ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    private void handleDcDimmingChange(boolean newState) {
        FileUtils.writeLine(DC_DIMMING_NODE, newState ? "1" : "0");
        
        if (newState) {
            disableHBMFeatures();
        }
        
        updatePreferences(newState);
        updateUI(newState);
    }

    private void disableHBMFeatures() {
        if (mSharedPrefs.getBoolean(HBM_ENABLE_KEY, false)) {
            disableHBM();
        }

        if (mSharedPrefs.getBoolean(AUTO_HBM_ENABLE_KEY, false)) {
            disableAutoHBM();
        }
    }

    private void disableHBM() {
        FileUtils.writeLine(HBM_NODE, "0");
        mSharedPrefs.edit().putBoolean(HBM_ENABLE_KEY, false).apply();
        broadcastStateChange(ACTION_HBM_CHANGED, false);
    }

    private void disableAutoHBM() {
        mSharedPrefs.edit().putBoolean(AUTO_HBM_ENABLE_KEY, false).apply();
        stopService(new Intent(this, AutoHBMService.class));
        broadcastStateChange(ACTION_AUTO_HBM_CHANGED, false);
    }

    private void updatePreferences(boolean newState) {
        mSharedPrefs.edit().putBoolean(DC_DIMMING_ENABLE_KEY, newState).apply();
        broadcastStateChange(ACTION_DC_CHANGED, newState);
    }

    private void broadcastStateChange(String action, boolean state) {
        Intent intent = new Intent(action);
        intent.putExtra("state", state);
        sendBroadcast(intent);
    }
}