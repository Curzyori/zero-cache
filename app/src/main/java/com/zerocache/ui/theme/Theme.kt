package com.zerocache.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = ZCPrimary,
    onPrimary = ZCOnPrimary,
    primaryContainer = ZCTintLavender,
    onPrimaryContainer = ZCPrimaryDeep,
    secondary = ZCPrimaryPressed,
    onSecondary = ZCOnPrimary,
    background = ZCCanvas,
    onBackground = ZCInk,
    surface = ZCCanvas,
    onSurface = ZCInk,
    surfaceVariant = ZCSurface,
    onSurfaceVariant = ZCSlate,
    outline = ZCHairlineStrong,
    outlineVariant = ZCHairline,
    error = ZCError,
    onError = ZCOnPrimary
)

private val DarkColors = darkColorScheme(
    primary = ZCPrimary,
    onPrimary = ZCOnPrimary,
    primaryContainer = ZCPrimaryDeep,
    onPrimaryContainer = ZCTintLavender,
    secondary = ZCPrimaryPressed,
    onSecondary = ZCOnPrimary,
    background = ZCSurfaceDark,
    onBackground = ZCCanvas,
    surface = ZCSurfaceDark,
    onSurface = ZCCanvas,
    surfaceVariant = ZCCharcoal,
    onSurfaceVariant = ZCMuted,
    outline = ZCSteel,
    outlineVariant = ZCCharcoal,
    error = ZCError,
    onError = ZCOnPrimary
)

@Composable
fun ZeroCacheTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = ZCTypography,
        content = content
    )
}
