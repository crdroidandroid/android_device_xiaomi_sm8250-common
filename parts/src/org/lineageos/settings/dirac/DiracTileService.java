/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.dirac;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import org.lineageos.settings.R;

public class DiracTileService extends TileService {
    private static final String TAG = "DiracTileService";
    private DiracUtils mUtils;
    private final Runnable mStateListener = this::updateTile;

    @Override
    public void onStartListening() {
        super.onStartListening();
        try {
            mUtils = DiracUtils.getInstance(getApplicationContext());
            mUtils.addListener(mStateListener);
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot initialize MiSound", e);
        }
        updateTile();
    }

    @Override
    public void onStopListening() {
        if (mUtils != null) mUtils.removeListener(mStateListener);
        super.onStopListening();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        int state = Tile.STATE_UNAVAILABLE;
        tile.setSubtitle(null);
        try {
            DiracUtils utils = DiracUtils.getInstance(getApplicationContext());
            if (utils.isAvailable()) {
                state = utils.isEnabledRequested() ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
                if (utils.isPausedForCommunication()) {
                    tile.setSubtitle(getString(R.string.dirac_paused_for_calls));
                }
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot read MiSound tile state", e);
        }
        tile.setState(state);
        tile.updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        // Toggle user intent, not temporary call bypass. A click during a call can turn it off.
        try {
            DiracUtils utils = DiracUtils.getInstance(getApplicationContext());
            if (!utils.isAvailable() || !utils.setEnabled(!utils.isEnabledRequested())) {
                Toast.makeText(this, R.string.dirac_apply_failed, Toast.LENGTH_SHORT).show();
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Cannot toggle MiSound", e);
            Toast.makeText(this, R.string.dirac_apply_failed, Toast.LENGTH_SHORT).show();
        }
        updateTile();
    }
}
