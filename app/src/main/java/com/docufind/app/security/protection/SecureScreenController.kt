package com.docufind.app.security.protection

import android.app.Activity
import android.view.WindowManager
import java.util.WeakHashMap

/**
 * Reference-counted FLAG_SECURE controller.
 *
 * Multiple protected Compose destinations can overlap briefly during navigation.
 * A simple enable-on-enter / disable-on-dispose effect can therefore expose one
 * frame when one destination disposes before the next has finished composing.
 */
object SecureScreenController {
    private val activeTokens = WeakHashMap<Activity, MutableSet<Any>>()

    @Synchronized
    fun acquire(activity: Activity, token: Any) {
        val tokens = activeTokens.getOrPut(activity) { mutableSetOf() }
        tokens.add(token)
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    @Synchronized
    fun release(activity: Activity, token: Any) {
        val tokens = activeTokens[activity] ?: return
        tokens.remove(token)
        if (tokens.isEmpty()) {
            activeTokens.remove(activity)
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    @Synchronized
    fun forceClear(activity: Activity) {
        activeTokens.remove(activity)
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    @Synchronized
    fun isSecure(activity: Activity): Boolean =
        activeTokens[activity]?.isNotEmpty() == true
}
