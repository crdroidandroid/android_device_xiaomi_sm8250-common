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

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import org.lineageos.settings.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BypassChargeTileService extends TileService {

    private static final String TAG = "BypassChargeTile";
    private ChargeUtils chargeUtils;
    private ExecutorService executorService;
    private Handler mainHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        chargeUtils = new ChargeUtils(this);
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        executorService.execute(() -> {
            boolean enabled = chargeUtils.isBypassChargeEnabled();
            mainHandler.post(() -> updateTileUI(enabled));
        });
    }

    @Override
    public void onClick() {
        super.onClick();

        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }

        int currentState = tile.getState();
        boolean newEnabled = currentState != Tile.STATE_ACTIVE;

        if (newEnabled) {
            executorService.execute(() -> {
                ChargeUtils.SafetyCheckResult safetyCheck = chargeUtils.performSafetyChecks();

                if (!safetyCheck.isSafe()) {
                    mainHandler.post(() -> {
                        Toast.makeText(this, 
                                getString(R.string.charge_bypass_safety_failed, 
                                        safetyCheck.getReason()),
                                Toast.LENGTH_LONG).show();
                        updateTileUI(false);
                    });
                    return;
                }

                chargeUtils.enableBypassCharge(true);
                mainHandler.post(() -> updateTileUI(true));
            });
        } else {
            updateTileUI(false);
            executorService.execute(() -> {
                chargeUtils.enableBypassCharge(false);
            });
        }
    }

    private void updateTileUI(boolean enabled) {
        Tile tile = getQsTile();
        if (tile != null) {
            tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.updateTile();
        }
    }

    public static void updateTile(Context context) {
        try {
            requestListeningState(
                    context,
                    new ComponentName(context, BypassChargeTileService.class)
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to request tile update", e);
        }
    }
}
