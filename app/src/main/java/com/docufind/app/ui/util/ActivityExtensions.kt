package com.docufind.app.ui.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity

fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun Context.findActivity(): Activity? = findFragmentActivity()

@Composable
fun rememberFragmentActivity(): FragmentActivity? {
    val context = LocalContext.current
    return remember(context) { context.findFragmentActivity() }
}
