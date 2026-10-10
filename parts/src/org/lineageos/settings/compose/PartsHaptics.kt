/* SPDX-License-Identifier: Apache-2.0 */
package org.lineageos.settings.compose

import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View

/** Touch feedback follows user settings; unsupported amplitude control uses platform detents. */
class PartsHaptics(private val view: View) {
    private val vibrator by lazy {
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    }
    private var lastDetent = Long.MIN_VALUE
    private val detent =
        VibrationEffect.createWaveform(longArrayOf(4, 6, 4), intArrayOf(18, 36, 0), -1)
    private val attributes =
        VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_TOUCH).build()

    fun selection() {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun detent() {
        val now = SystemClock.uptimeMillis()
        if (lastDetent != Long.MIN_VALUE && now - lastDetent < 40) return
        lastDetent = now
        if (
            !view.isHapticFeedbackEnabled ||
                Settings.System.getInt(
                    view.context.contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    1,
                ) == 0
        )
            return
        val device = vibrator
        if (device?.hasVibrator() != true || !device.hasAmplitudeControl()) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
            return
        }
        try {
            device.vibrate(detent, attributes)
        } catch (_: RuntimeException) {
            view.performHapticFeedback(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        }
    }
}
