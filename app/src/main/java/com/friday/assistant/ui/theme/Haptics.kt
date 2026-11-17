package com.friday.assistant.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Restrained haptic accents for choreographed moments. Every call is a
 * no-op under reduced motion, so accessibility settings silence the whole
 * system. Deliberately limited to four high-value touchpoints; never used
 * for continuous or per-frame feedback.
 */
object FridayHaptics {

    /** Subtle scalar tick: boot dock, prompt card taps, HUD sheet reveal. */
    fun tickLight(view: View, reducedMotion: Boolean) {
        if (reducedMotion) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Firmer confirmation tick: generation completion lock-in. */
    fun tickConfirm(view: View, reducedMotion: Boolean) {
        if (reducedMotion) return
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
}
