/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings

import android.app.Application
import android.util.Log
import org.lineageos.settings.misound.MiSoundUtils

class PartsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            MiSoundUtils.getInstance(this)
        } catch (error: RuntimeException) {
            Log.w("XiaomiParts", "Cannot initialize MiSound", error)
        }
    }
}
