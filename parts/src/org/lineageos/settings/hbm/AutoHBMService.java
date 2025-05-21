package org.lineageos.settings.hbm;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.utils.FileUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AutoHBMService extends Service {
    // System nodes
    private static final String DC_DIMMING_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/msm_fb_ea_enable";
    private static final String HBM_NODE = "/sys/devices/platform/soc/soc:qcom,dsi-display-primary/hbm";
    private static final String BACKLIGHT_NODE = "/sys/class/backlight/panel0-backlight/brightness";

    // Preference keys
    private static final String HBM_ENABLE_KEY = "hbm";
    private static final String AUTO_HBM_ENABLE_KEY = "auto_hbm";
    private static final String AUTO_HBM_THRESHOLD_KEY = "auto_hbm_threshold";
    private static final String AUTO_HBM_DISABLE_TIME_KEY = "auto_hbm_disable_time";

    // Intent actions
    private static final String ACTION_HBM_CHANGED = "org.lineageos.settings.device.HBM_CHANGED";

    // Default values
    private static final String DEFAULT_LUX_THRESHOLD = "7000";
    private static final String DEFAULT_DISABLE_TIME = "1";

    // Service state
    private boolean mAutoHBMActive = false;
    private int mPreviousBrightness;
    private ExecutorService mExecutorService;
    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private SharedPreferences mSharedPrefs;

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float currentLux = event.values[0];
            float luxThreshold = getLuxThreshold();
            
            if (!isKeyguardShowing()) {
                handleLuxChange(currentLux, luxThreshold);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not needed
        }
    };

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    activateLightSensorRead();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    deactivateLightSensorRead();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initializeService();
    }

    private void initializeService() {
        mExecutorService = Executors.newSingleThreadExecutor();
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        registerScreenStateReceiver();
        initializeLightSensorIfNeeded();
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void initializeLightSensorIfNeeded() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isInteractive()) {
            activateLightSensorRead();
        }
    }

    private void activateLightSensorRead() {
        submit(() -> {
            mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
            mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mSensorManager.registerListener(mSensorEventListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        });
    }

    private void deactivateLightSensorRead() {
        submit(() -> {
            if (mSensorManager != null) {
                mSensorManager.unregisterListener(mSensorEventListener);
                mAutoHBMActive = false;
                enableHBM(false);
            }
        });
    }

    private void handleLuxChange(float currentLux, float threshold) {
        if (currentLux > threshold) {
            if (!mAutoHBMActive || !isHBMEnabled()) {
                mAutoHBMActive = true;
                enableHBM(true);
            }
        } else if (mAutoHBMActive) {
            scheduleHBMDisable(currentLux, threshold);
        }
    }

    private void scheduleHBMDisable(float currentLux, float threshold) {
        long disableDelay = getDisableTimeInSeconds() * 1000;
        mExecutorService.submit(() -> {
            try {
                Thread.sleep(disableDelay);
                if (currentLux < threshold) {
                    mAutoHBMActive = false;
                    enableHBM(false);
                }
            } catch (InterruptedException ignored) {
                // Handle interruption if needed
            }
        });
    }

    private void enableHBM(boolean newState) {
        if (newState) {
            // Store current brightness before enabling HBM
            mPreviousBrightness = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 255);
            
            FileUtils.writeLine(HBM_NODE, "1");
            FileUtils.writeLine(BACKLIGHT_NODE, "2047");
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
        } else {
            FileUtils.writeLine(HBM_NODE, "0");
            // Restore previous brightness
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 
                mPreviousBrightness);
        }

        broadcastHBMState(newState);
    }

    private void broadcastHBMState(boolean state) {
        Intent intent = new Intent(ACTION_HBM_CHANGED);
        intent.putExtra("state", state);
        sendBroadcast(intent);
    }

    private boolean isHBMEnabled() {
        return FileUtils.getFileValueAsBoolean(HBM_NODE, false);
    }

    private boolean isKeyguardShowing() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return km != null && km.inKeyguardRestrictedInputMode();
    }

    private float getLuxThreshold() {
        return Float.parseFloat(mSharedPrefs.getString(AUTO_HBM_THRESHOLD_KEY, DEFAULT_LUX_THRESHOLD));
    }

    private long getDisableTimeInSeconds() {
        return Long.parseLong(mSharedPrefs.getString(AUTO_HBM_DISABLE_TIME_KEY, DEFAULT_DISABLE_TIME));
    }

    private void disableAutoHBMAndStop() {
        mSharedPrefs.edit().putBoolean(AUTO_HBM_ENABLE_KEY, false).apply();
        stopSelf();
    }

    private Future<?> submit(Runnable runnable) {
        return mExecutorService.submit(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mScreenStateReceiver);
        deactivateLightSensorRead();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}