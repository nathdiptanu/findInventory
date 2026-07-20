package com.docufind.app.security.protection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.docufind.app.ui.util.rememberFragmentActivity

/** Always blocks screenshots/recents preview on sensitive screens. */
@Composable
fun ForceSecureScreenEffect() {
    val activity = rememberFragmentActivity()
    val token = remember { Any() }
    DisposableEffect(activity) {
        activity?.let { SecureScreenController.acquire(it, token) }
        onDispose { activity?.let { SecureScreenController.release(it, token) } }
    }
}
