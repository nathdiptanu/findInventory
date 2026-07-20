package com.docufind.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = DocuFindBlue,
    onPrimary = DocuFindWhite,
    primaryContainer = DocuFindBlueLight,
    onPrimaryContainer = DocuFindDeepBlue,
    secondary = DocuFindNavy,
    onSecondary = DocuFindWhite,
    secondaryContainer = DocuFindBlueLight,
    onSecondaryContainer = DocuFindDeepBlue,
    tertiary = DocuFindTeal,
    onTertiary = DocuFindWhite,
    tertiaryContainer = DocuFindTealLight,
    onTertiaryContainer = DocuFindNavy,
    background = DocuFindBlueSurface,
    onBackground = DocuFindTextPrimary,
    surface = DocuFindWhite,
    onSurface = DocuFindTextPrimary,
    surfaceVariant = DocuFindCardBackground,
    onSurfaceVariant = DocuFindTextSecondary,
    outline = DocuFindCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DocuFindBlue,
    onPrimary = DocuFindWhite,
    primaryContainer = DocuFindNavy,
    onPrimaryContainer = DocuFindBlueLight,
    secondary = DocuFindBlueLight,
    onSecondary = DocuFindNavy,
    tertiary = DocuFindTeal,
    onTertiary = DocuFindWhite,
    background = DocuFindVaultBackground,
    onBackground = DocuFindWhite,
    surface = DocuFindVaultBackground,
    onSurface = DocuFindWhite,
    surfaceVariant = DocuFindNavy,
    onSurfaceVariant = DocuFindBlueLight,
    outline = DocuFindTextSecondary
)

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@Composable
fun DocuFindTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            view.context.findActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DocuFindTypography,
        content = content
    )
}
