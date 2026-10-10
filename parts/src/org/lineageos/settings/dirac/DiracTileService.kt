/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.dirac

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import org.lineageos.settings.R

class DiracTileService : TileService() {
    private var utils: DiracUtils? = null
    private val listener = Runnable { updateTile() }

    override fun onStartListening() {
        super.onStartListening()
        try {
            utils = DiracUtils.getInstance(applicationContext)
            utils?.addListener(listener)
        } catch (error: RuntimeException) {
            Log.w("DiracTile", "Cannot initialize MiSound", error)
        }
        updateTile()
    }

    override fun onStopListening() {
        utils?.removeListener(listener)
        super.onStopListening()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        tile.state = Tile.STATE_UNAVAILABLE
        tile.subtitle = null
        try {
            val owner = DiracUtils.getInstance(applicationContext)
            if (owner.isAvailable()) {
                tile.state =
                    if (owner.isEnabledRequested()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                if (owner.isPausedForCommunication())
                    tile.subtitle = getString(R.string.dirac_paused_for_calls)
            }
        } catch (error: RuntimeException) {
            Log.w("DiracTile", "Cannot read tile state", error)
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        try {
            val owner = DiracUtils.getInstance(applicationContext)
            check(owner.isAvailable() && owner.setEnabled(!owner.isEnabledRequested()))
        } catch (error: RuntimeException) {
            Log.w("DiracTile", "Cannot toggle MiSound", error)
            Toast.makeText(this, R.string.dirac_apply_failed, Toast.LENGTH_SHORT).show()
        }
        updateTile()
    }
}
