package com.docufind.app.ui.util

import android.content.Context
import android.provider.Settings

fun Context.isReducedMotionEnabled(): Boolean {
    val animatorScale = Settings.Global.getFloat(
        contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    val transitionScale = Settings.Global.getFloat(
        contentResolver,
        Settings.Global.TRANSITION_ANIMATION_SCALE,
        1f
    )
    return animatorScale == 0f || transitionScale == 0f
}
