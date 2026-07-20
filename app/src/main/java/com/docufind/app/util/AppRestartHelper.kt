package com.docufind.app.util

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess

object AppRestartHelper {
    fun restartApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launchIntent)
        exitProcess(0)
    }
}
