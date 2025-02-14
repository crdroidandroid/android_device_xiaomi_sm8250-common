package org.lineageos.settings.hbm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;

public class AutoHBMModeTileService extends TileService {

    // System nodes
    private static final String DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";

    // Preference keys
    private static final String DC_DIMMING_ENABLE_KEY = "dc_dimming_enable";
    private static final String HBM_ENABLE_KEY = "hbm";
    private static final String AUTO_HBM_ENABLE_KEY = "auto_hbm";

    // Intent actions
    private static final String ACTION_HBM_CHANGED = "org.lineageos.settings.device.HBM_CHANGED";
    private static final String ACTION_AUTO_HBM_CHANGED = "org.lineageos.settings.device.AUTO_HBM_CHANGED";
    private static final String ACTION_DC_CHANGED = "org.lineageos.settings.device.DC_CHANGED";

    // System settings
    private static final int MAX_BRIGHTNESS = 255;
    private static final String MAX_BACKLIGHT = "2047";

    private SharedPreferences mSharedPrefs;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_AUTO_HBM_CHANGED.equals(intent.getAction())) {
                boolean newState = intent.getBooleanExtra("state", false);
                updateTileState(newState);
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
        registerAUTOHBMReceiver();
        updateTileState(isAUTOHBMEnabled());
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onClick() {
        super.onClick();
        toggleAUTOHBMState();
    }

    private void registerAUTOHBMReceiver() {
        IntentFilter filter = new IntentFilter(ACTION_AUTO_HBM_CHANGED);
        registerReceiver(mReceiver, filter);
    }

    private void updateTileState(boolean newState) {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(newState ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    private boolean isAUTOHBMEnabled() {
        return mSharedPrefs.getBoolean(AUTO_HBM_ENABLE_KEY, false);
    }

    private void toggleAUTOHBMState() {
        boolean newState = !isAUTOHBMEnabled();
        SharedPreferences.Editor editor = mSharedPrefs.edit();

        if (newState) {
            handleAUTOHBMEnable(editor);
        }

        editor.putBoolean(AUTO_HBM_ENABLE_KEY, newState).apply();
        broadcastStateChange(ACTION_AUTO_HBM_CHANGED, newState);
        FileUtils.enableService(this);
        updateTileState(newState);
    }

    private void handleAUTOHBMEnable(SharedPreferences.Editor editor) {
        // Disable HBM if enabled
        if (mSharedPrefs.getBoolean(HBM_ENABLE_KEY, false)) {
            FileUtils.writeLine(HBM_NODE, "0");
            editor.putBoolean(HBM_ENABLE_KEY, false);
            broadcastStateChange(ACTION_HBM_CHANGED, false);
        }

        // Disable DC Dimming if enabled
        if (mSharedPrefs.getBoolean(DC_DIMMING_ENABLE_KEY, false)) {
            FileUtils.writeLine(DC_DIMMING_NODE, "0");
            editor.putBoolean(DC_DIMMING_ENABLE_KEY, false);
            broadcastStateChange(ACTION_DC_CHANGED, false);
        }
    }

    private void broadcastStateChange(String action, boolean state) {
        Intent intent = new Intent(action);
        intent.putExtra("state", state);
        sendBroadcast(intent);
    }
}