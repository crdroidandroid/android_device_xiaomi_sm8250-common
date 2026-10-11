/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.misound

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import kotlinx.coroutines.*
import org.lineageos.settings.R

open class MiSoundTileService : TileService() {
    private val utils by lazy { MiSoundUtils.getInstance(applicationContext) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listening?.cancel()
        utils.requestRefresh()
        listening = scope.launch {
            utils.state.collect { state ->
                qsTile?.apply {
                    this.state = when {
                        !state.available -> Tile.STATE_UNAVAILABLE
                        state.enabled -> Tile.STATE_ACTIVE
                        else -> Tile.STATE_INACTIVE
                    }
                    subtitle = if (state.paused) getString(R.string.misound_paused_for_calls) else null
                    updateTile()
                }
            }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val app = applicationContext
        utils.submit({ check(it.setEnabled(!it.isEnabledRequested())) }) {
            Toast.makeText(app, R.string.misound_apply_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
