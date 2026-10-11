/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.misound

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import org.lineageos.settings.R

open class MiSoundTileService : TileService() {
    private var utils: MiSoundUtils? = null
    private val listener = Runnable { updateTile() }

    override fun onStartListening() {
        super.onStartListening()
        try {
            utils = MiSoundUtils.getInstance(applicationContext)
            utils?.addListener(listener)
        } catch (error: RuntimeException) {
            Log.w("MiSoundTile", "Cannot initialize MiSound", error)
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
            val owner = MiSoundUtils.getInstance(applicationContext)
            if (owner.isAvailable()) {
                tile.state =
                    if (owner.isEnabledRequested()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                if (owner.isPausedForCommunication())
                    tile.subtitle = getString(R.string.misound_paused_for_calls)
            }
        } catch (error: RuntimeException) {
            Log.w("MiSoundTile", "Cannot read tile state", error)
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        try {
            val owner = MiSoundUtils.getInstance(applicationContext)
            check(owner.isAvailable() && owner.setEnabled(!owner.isEnabledRequested()))
        } catch (error: RuntimeException) {
            Log.w("MiSoundTile", "Cannot toggle MiSound", error)
            Toast.makeText(this, R.string.misound_apply_failed, Toast.LENGTH_SHORT).show()
        }
        updateTile()
    }
}
