/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.utils

import android.content.Context

/** Preserve AndroidX default preferences, including the existing device-protected context. */
object PartsPreferences {
    fun getDefaultSharedPreferences(context: Context) =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
}
