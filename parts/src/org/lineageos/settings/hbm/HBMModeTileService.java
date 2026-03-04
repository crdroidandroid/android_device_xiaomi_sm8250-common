package org.lineageos.settings.hbm;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import androidx.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;
import org.lineageos.settings.R;
import org.lineageos.settings.utils.FileUtils;

public class HBMModeTileService extends TileService {

    private static final String DC_DIMMING_KEY = "dc_dimming";
    private static final String HBM_KEY = "hbm";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";

    private BroadcastReceiver mScreenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                boolean turnOffOnScreenOff = prefs.getBoolean(
                        HBMFragment.HBM_TURN_OFF_ON_SCREEN_OFF_KEY, false);
                boolean hbmEnabled = prefs.getBoolean(HBM_KEY, false);

                if (turnOffOnScreenOff && hbmEnabled) {
                    FileUtils.writeLine(HBM_NODE, "0");

                    int lastBrightness = prefs.getInt("last_brightness", 128);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, lastBrightness);

                    int lastBrightnessMode = prefs.getInt("last_brightness_mode",
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE, lastBrightnessMode);

                    prefs.edit()
                            .putBoolean(HBM_KEY, false)
                            .remove("last_brightness")
                            .remove("last_brightness_mode")
                            .apply();

                    updateUI(false);
                }
            }
        }
    };

    private void updateUI(boolean enabled) {
        final Tile tile = getQsTile();
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(mScreenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenOffReceiver);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        updateUI(sharedPrefs.getBoolean(HBM_KEY, false));
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
    }

    @Override
    public void onClick() {
        super.onClick();

        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(this);

        final boolean dcDimmingEnabled =
                sharedPrefs.getBoolean(DC_DIMMING_KEY, false);

        if (dcDimmingEnabled) {
            Toast.makeText(
                    this,
                    R.string.hbm_disable_dc_dimming_first,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        final boolean enabled =
                !sharedPrefs.getBoolean(HBM_KEY, false);

        if (enabled) {
            // Save current brightness and auto brightness mode
            int currentBrightness = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
            );
            int currentBrightnessMode = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );
            sharedPrefs.edit()
                    .putInt("last_brightness", currentBrightness)
                    .putInt("last_brightness_mode", currentBrightnessMode)
                    .apply();

            // Disable auto brightness to prevent conflict
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            );

            FileUtils.writeLine(HBM_NODE, "1");
            FileUtils.writeLine(BACKLIGHT_NODE, "2047");
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    255
            );
        } else {
            FileUtils.writeLine(HBM_NODE, "0");

            // Restore brightness
            int lastBrightness =
                    sharedPrefs.getInt("last_brightness", 128);
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    lastBrightness
            );

            // Restore auto brightness mode
            int lastBrightnessMode = sharedPrefs.getInt("last_brightness_mode",
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(
                    getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    lastBrightnessMode
            );
        }

        sharedPrefs.edit().putBoolean(HBM_KEY, enabled).apply();
        updateUI(enabled);
    }
}