package com.docufind.app.ui.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/** Respects status bar, nav bar, display cutouts, and gesture insets. */
fun Modifier.docuFindSafeArea(): Modifier = composed {
    windowInsetsPadding(WindowInsets.safeDrawing)
}

/** Bottom inset only — use on footers and primary actions above the nav bar. */
fun Modifier.docuFindNavBarPadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.navigationBars)
}

/** Top inset only — use when bottom padding is handled separately. */
fun Modifier.docuFindStatusBarPadding(): Modifier = composed {
    windowInsetsPadding(WindowInsets.statusBars)
}
