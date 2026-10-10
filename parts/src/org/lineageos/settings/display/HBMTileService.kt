/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.display

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import org.lineageos.settings.R

class HBMTileService : TileService() {
    private fun updateUi() {
        val tile = qsTile ?: return
        tile.state =
            if (!DisplayUtils.isHbmSupported()) Tile.STATE_UNAVAILABLE
            else if (DisplayUtils.isHbmEnabled()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateUi()
    }

    override fun onClick() {
        super.onClick()
        if (
            DisplayUtils.isHbmSupported() &&
                !DisplayUtils.setHbm(this, !DisplayUtils.isHbmEnabled())
        )
            Toast.makeText(this, R.string.parts_apply_failed, Toast.LENGTH_SHORT).show()
        updateUi()
    }
}
