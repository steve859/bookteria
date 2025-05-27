package com.soft.bookteria.helpers

import android.view.HapticFeedbackConstants
import android.view.View

fun View.weakHapticFeedback() {
    this.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}