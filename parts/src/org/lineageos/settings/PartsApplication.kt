/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings

import android.app.Application
import android.util.Log
import org.lineageos.settings.dirac.DiracUtils

class PartsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            DiracUtils.getInstance(this)
        } catch (error: RuntimeException) {
            Log.w("XiaomiParts", "Cannot initialize MiSound", error)
        }
    }
}
