/*
 * Copyright (C) 2018,2020 The LineageOS Project
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

package org.lineageos.settings.dirac;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Process-wide owner of MiSound settings and the user's processing/recovery request. */
public class DiracUtils {
    private static final String TAG = "DiracUtils";
    static final String PREF_ENABLE = "dirac_enable";
    static final String PREF_HEADSET = "dirac_headset_pref";
    static final String PREF_PRESET = "dirac_preset_pref";
    static final String PREF_SCENE = "scenario_selection";
    static final String PREF_HIFI = "dirac_hifi_pref";
    static final String PREF_EQ = "dirac_eq_enabled";
    static final String PREF_PAUSE = "dirac_pause_during_calls";
    private static final String FLAT_PRESET = "0,0,0,0,0,0,0";

    private static DiracUtils sInstance;
    private final SharedPreferences mPreferences;
    private final AudioManager mAudioManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Set<Runnable> mListeners = new HashSet<>();
    private boolean mServerDown;
    private boolean mCommunicationActive;
    private Boolean mAppliedEnabled;
    private DiracSound mSound;
    private int mRestoreAttempts;
    private final Runnable mRestore = this::restore;
    private final Runnable mAudioStateChanged = this::updateAudioState;
    private final AudioManager.OnModeChangedListener mModeListener = mode -> scheduleAudioUpdate();
    private final AudioManager.AudioPlaybackCallback mPlaybackCallback =
            new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    scheduleAudioUpdate();
                }
            };
    private final AudioManager.AudioRecordingCallback mRecordingCallback =
            new AudioManager.AudioRecordingCallback() {
                @Override
                public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
                    scheduleAudioUpdate();
                }
            };

    private DiracUtils(Context context) {
        Context appContext = context.getApplicationContext().createDeviceProtectedStorageContext();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        mAudioManager = appContext.getSystemService(AudioManager.class);
        mAudioManager.setAudioServerStateCallback(appContext.getMainExecutor(),
                new AudioManager.AudioServerStateCallback() {
                    @Override
                    public void onAudioServerDown() {
                        synchronized (DiracUtils.this) {
                            mServerDown = true;
                            mHandler.removeCallbacks(mRestore);
                            mHandler.removeCallbacks(mAudioStateChanged);
                            releaseEffect();
                            notifyListeners();
                        }
                    }

                    @Override
                    public void onAudioServerUp() {
                        synchronized (DiracUtils.this) {
                            mServerDown = false;
                            mRestoreAttempts = 0;
                            restore();
                        }
                    }
                });
        // This owner lives for the persistent Parts process, not a Settings activity.
        try {
            mAudioManager.addOnModeChangedListener(appContext.getMainExecutor(), mModeListener);
            mAudioManager.registerAudioPlaybackCallback(mPlaybackCallback, mHandler);
            mAudioManager.registerAudioRecordingCallback(mRecordingCallback, mHandler);
        } catch (RuntimeException failure) {
            cleanupAfterFailure(failure,
                    () -> mAudioManager.removeOnModeChangedListener(mModeListener));
            cleanupAfterFailure(failure,
                    () -> mAudioManager.unregisterAudioPlaybackCallback(mPlaybackCallback));
            cleanupAfterFailure(failure,
                    () -> mAudioManager.unregisterAudioRecordingCallback(mRecordingCallback));
            cleanupAfterFailure(failure, mAudioManager::clearAudioServerStateCallback);
            mHandler.removeCallbacks(mAudioStateChanged);
            throw failure;
        }
        restore();
    }

    private static void cleanupAfterFailure(RuntimeException failure, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException error) {
            if (failure != error) failure.addSuppressed(error);
        }
    }

    public static synchronized DiracUtils getInstance(Context context) {
        if (sInstance == null) sInstance = new DiracUtils(context);
        return sInstance;
    }

    private synchronized void scheduleAudioUpdate() {
        mHandler.removeCallbacks(mAudioStateChanged);
        mHandler.post(mAudioStateChanged);
    }

    private boolean effectiveEnabled(boolean requested, boolean pauseDuringCalls) {
        int mode = mAudioManager.getMode();
        mCommunicationActive = mode != AudioManager.MODE_NORMAL && mode != AudioManager.MODE_RINGTONE;
        if (!mCommunicationActive) {
            for (AudioPlaybackConfiguration config : mAudioManager.getActivePlaybackConfigurations()) {
                if (config.getPlayerState() == AudioPlaybackConfiguration.PLAYER_STATE_STARTED
                        && config.getAudioAttributes().getUsage()
                        == AudioAttributes.USAGE_VOICE_COMMUNICATION) {
                    mCommunicationActive = true;
                    break;
                }
            }
        }
        if (!mCommunicationActive) {
            for (AudioRecordingConfiguration config : mAudioManager.getActiveRecordingConfigurations()) {
                if (!config.isClientSilenced() && config.getClientAudioSource()
                        == MediaRecorder.AudioSource.VOICE_COMMUNICATION) {
                    mCommunicationActive = true;
                    break;
                }
            }
        }
        return requested && !(pauseDuringCalls && mCommunicationActive);
    }

    private synchronized void updateAudioState() {
        if (mServerDown || (mSound == null && mRestoreAttempts >= 5)) return;
        try {
            requireEffect();
            boolean enabled = effectiveEnabled(isEnabledRequested(), isPauseDuringCallsEnabled());
            // Playback callbacks also arrive for ordinary media. Avoid redundant DSP writes.
            if (mAppliedEnabled == null || mAppliedEnabled != enabled) {
                applyEnabled(enabled);
                notifyListeners();
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Cannot update MiSound communication bypass", error);
            restoreAfterFailure();
        }
    }

    private synchronized void restore() {
        try {
            requireEffect();
            mRestoreAttempts = 0;
            mHandler.removeCallbacks(mRestore);
            notifyListeners();
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot restore MiSound", e);
            if (++mRestoreAttempts < 5 && !mServerDown) {
                mHandler.removeCallbacks(mRestore);
                mHandler.postDelayed(mRestore, 1000);
            }
            notifyListeners();
        }
    }

    public synchronized void addListener(Runnable listener) {
        mListeners.add(listener);
    }

    public synchronized void removeListener(Runnable listener) {
        mListeners.remove(listener);
        mHandler.removeCallbacks(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : mListeners) {
            mHandler.removeCallbacks(listener);
            mHandler.post(listener);
        }
    }

    private void releaseEffect() {
        DiracSound sound = mSound;
        mSound = null;
        mAppliedEnabled = null;
        if (sound == null) return;
        try {
            if (!mServerDown && sound.hasControl()) {
                try {
                    sound.setEnabled(false);
                } finally {
                    sound.setMusic(0);
                }
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Cannot bypass MiSound before release", error);
        } finally {
            try {
                sound.release();
            } catch (RuntimeException error) {
                Log.w(TAG, "Cannot release MiSound", error);
            }
        }
    }

    private DiracSound requireEffect() {
        if (mServerDown) throw new IllegalStateException("Audio server is down");
        try {
            if (mSound != null && mSound.hasControl()) return mSound;
            releaseEffect();
            mSound = new DiracSound(0, 0);
            if (!mSound.hasControl()) throw new IllegalStateException("MiSound control unavailable");
            // Establish bypass before writing settings to an inherited native instance.
            applyEnabled(false);
            // An Off request must not depend on parsing or applying saved tuning.
            if (isEnabledRequested()) {
                applySettings();
                applyEnabled(effectiveEnabled(true, isPauseDuringCallsEnabled()));
            }
            return mSound;
        } catch (RuntimeException e) {
            releaseEffect();
            throw e;
        }
    }

    private String safeString(String key, String fallback) {
        try {
            String value = mPreferences.getString(key, fallback);
            return value == null ? fallback : value;
        } catch (ClassCastException e) {
            mPreferences.edit().remove(key).apply();
            return fallback;
        }
    }

    private boolean safeBoolean(String key, boolean fallback) {
        try {
            return mPreferences.getBoolean(key, fallback);
        } catch (ClassCastException e) {
            mPreferences.edit().remove(key).apply();
            return fallback;
        }
    }

    private int safeIntString(String key, String fallback, int min, int max) {
        String value = safeString(key, fallback);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed >= min && parsed <= max) return parsed;
        } catch (NumberFormatException ignored) {
        }
        mPreferences.edit().remove(key).apply();
        return Integer.parseInt(fallback);
    }

    private String safePreset() {
        String preset = safeString(PREF_PRESET, FLAT_PRESET);
        try {
            parseLevels(preset);
            return preset;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Discarding invalid saved MiSound preset", e);
            mPreferences.edit().remove(PREF_PRESET).apply();
            return FLAT_PRESET;
        }
    }

    // Share validation with the UI: an Off handle deliberately skips applySettings.
    public synchronized int getHeadsetType() {
        return safeIntString(PREF_HEADSET, "0", 0, 255);
    }

    public synchronized String getSavedPreset() {
        return safePreset();
    }

    public synchronized int getScenario() {
        return safeIntString(PREF_SCENE, "4", 0, 4);
    }

    private void applySettings() {
        mSound.setHeadsetType(getHeadsetType());
        applyLevel(getSavedPreset(), isEqualizerEnabled());
        mSound.setScenario(getScenario());
        applyHifi(getHifiMode());
    }

    private void applyEnabled(boolean enable) {
        if (enable) {
            mSound.setMusic(1);
            int status = mSound.setEnabled(true);
            if (status != AudioEffect.SUCCESS) {
                throw new IllegalStateException("MiSound setEnabled failed: " + status);
            }
        } else {
            RuntimeException failure = null;
            try {
                int status = mSound.setEnabled(false);
                if (status != AudioEffect.SUCCESS) {
                    throw new IllegalStateException("MiSound disable failed: " + status);
                }
            } catch (RuntimeException error) {
                failure = error;
            }
            try {
                mSound.setMusic(0);
            } catch (RuntimeException error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
            if (failure != null) throw failure;
        }
        if (!mSound.hasExpectedEnabledState(enable) || mSound.getMusic() != (enable ? 1 : 0)) {
            throw new IllegalStateException("MiSound enable state did not apply");
        }
        mAppliedEnabled = enable;
    }

    public synchronized void restoreAfterFailure() {
        releaseEffect();
        mRestoreAttempts = 0;
        restore();
    }

    public synchronized boolean isAvailable() {
        try {
            requireEffect();
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "MiSound is unavailable", e);
            return false;
        }
    }

    public synchronized boolean isDiracEnabled() {
        try {
            DiracSound sound = requireEffect();
            return sound.getEnabled() && sound.getMusic() == 1;
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot read MiSound state", e);
            releaseEffect();
            return false;
        }
    }

    public synchronized boolean isEnabledRequested() {
        return safeBoolean(PREF_ENABLE, false);
    }

    public synchronized boolean isEqualizerEnabled() {
        return safeBoolean(PREF_EQ, true);
    }

    public synchronized boolean isPauseDuringCallsEnabled() {
        return safeBoolean(PREF_PAUSE, true);
    }

    public synchronized boolean isPausedForCommunication() {
        return isEnabledRequested() && isPauseDuringCallsEnabled() && mCommunicationActive;
    }

    public synchronized boolean setEnabled(boolean enable) {
        // Persist shutdown intent before any operation that can fail. A failed Off
        // still returns false, but recovery must not resurrect the previous On state.
        if (!enable) mPreferences.edit().putBoolean(PREF_ENABLE, false).apply();
        try {
            requireEffect();
            applyEnabled(false);
            if (enable) {
                // A handle acquired while Off deliberately has no restored tuning.
                applySettings();
                applyEnabled(effectiveEnabled(true, isPauseDuringCallsEnabled()));
            }
            mPreferences.edit().putBoolean(PREF_ENABLE, enable).apply();
            notifyListeners();
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot change MiSound state", e);
            restoreAfterFailure();
            return false;
        }
    }

    public synchronized void setEqualizerEnabled(boolean enabled) {
        requireEffect();
        applyLevel(safePreset(), enabled);
        mPreferences.edit().putBoolean(PREF_EQ, enabled).apply();
        notifyListeners();
    }

    public synchronized void setPauseDuringCallsEnabled(boolean enabled) {
        requireEffect();
        applyEnabled(effectiveEnabled(isEnabledRequested(), enabled));
        mPreferences.edit().putBoolean(PREF_PAUSE, enabled).apply();
        notifyListeners();
    }

    public synchronized void setLevel(String preset) {
        parseLevels(preset); // Reject invalid new selections even while EQ is bypassed.
        requireEffect();
        applyLevel(preset, isEqualizerEnabled());
        mPreferences.edit().putString(PREF_PRESET, preset).apply();
        notifyListeners();
    }

    private static float[] parseLevels(String preset) {
        if (preset == null) throw new IllegalArgumentException("Missing MiSound preset");
        String[] values = preset.split(",", -1);
        if (values.length != 7) throw new IllegalArgumentException("Expected seven MiSound bands");
        float[] levels = new float[values.length];
        for (int band = 0; band < values.length; band++) {
            levels[band] = Float.parseFloat(values[band].trim());
            if (!Float.isFinite(levels[band])) {
                throw new IllegalArgumentException("Invalid MiSound band gain");
            }
        }
        return levels;
    }

    private void applyLevel(String preset, boolean enabled) {
        // Bypass is always a neutral curve, including after a malformed saved preset.
        float[] levels = enabled ? parseLevels(preset) : new float[0];
        for (int band = 0; band < DiracSound.EQ_BAND_COUNT; band++) {
            // Bypass only EQ; preserve headset/scene processing and the saved preset.
            mSound.setLevel(band, band < levels.length ? levels[band] : 0f);
        }
    }

    public synchronized int[] getSupportedHeadsets() {
        return requireEffect().getHeadsetList();
    }

    public synchronized void setHeadsetType(int value) {
        requireEffect().setHeadsetType(value);
        mPreferences.edit().putString(PREF_HEADSET, Integer.toString(value)).apply();
        notifyListeners();
    }

    public synchronized boolean getHifiMode() {
        return safeBoolean(PREF_HIFI, false);
    }

    public synchronized void setHifiMode(int value) {
        if (!isHifiSupported()) {
            throw new UnsupportedOperationException("HAL Hi-Fi feature is disabled");
        }
        requireEffect();
        applyHifi(value != 0);
        mPreferences.edit().putBoolean(PREF_HIFI, value != 0).apply();
        notifyListeners();
    }

    public boolean isHifiSupported() {
        return android.os.SystemProperties.getBoolean(
                "vendor.audio.feature.hifi_audio.enable", false);
    }

    private void applyHifi(boolean enabled) {
        if (!isHifiSupported()) return;
        mSound.setHifiMode(enabled ? 1 : 0);
        mAudioManager.setParameters("hifi_mode=" + enabled);
    }

    public synchronized void setScenario(int value) {
        requireEffect().setScenario(value);
        mPreferences.edit().putString(PREF_SCENE, Integer.toString(value)).apply();
        notifyListeners();
    }
}
