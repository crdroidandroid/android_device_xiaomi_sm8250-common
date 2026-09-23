/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings;

import android.app.Application;
import android.util.Log;

import org.lineageos.settings.dirac.DiracUtils;

public class PartsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Persistent processes can be recreated without another boot broadcast.
        try {
            DiracUtils.getInstance(this);
        } catch (RuntimeException e) {
            Log.w("XiaomiParts", "Cannot initialize MiSound", e);
        }
    }
}
