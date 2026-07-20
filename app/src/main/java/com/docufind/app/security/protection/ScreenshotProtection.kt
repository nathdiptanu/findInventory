package com.docufind.app.security.protection

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.docufind.app.security.settings.SecurityPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object ScreenshotProtection {
    fun enable(activity: Activity) {
        SecureScreenController.acquire(activity, ScreenshotProtection)
    }

    fun disable(activity: Activity) {
        SecureScreenController.release(activity, ScreenshotProtection)
    }

    internal fun setWindowSecure(activity: Activity) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    internal fun clearWindowSecure(activity: Activity) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    fun applyIfNeeded(activity: Activity, allowScreenshots: Boolean, forceSecure: Boolean) {
        if (forceSecure || !allowScreenshots) {
            enable(activity)
        } else {
            disable(activity)
        }
    }
}

@Composable
fun SecureScreenEffect(
    securityPreferences: SecurityPreferences,
    forceSecure: Boolean = false
) {
    val activity = LocalContext.current as? Activity ?: return
    val token = remember { Any() }
    DisposableEffect(forceSecure) {
        val allowScreenshots = runBlocking {
            securityPreferences.preferences.first().allowScreenshots
        }
        if (forceSecure || !allowScreenshots) {
            SecureScreenController.acquire(activity, token)
        }
        onDispose {
            SecureScreenController.release(activity, token)
        }
    }
}
