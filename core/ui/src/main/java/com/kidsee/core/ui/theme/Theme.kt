package com.kidsee.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = WazeBlue,
    onPrimary = Color.White,
    primaryContainer = WazeBlueLight,
    onPrimaryContainer = WazeBlueDark,
    secondary = WazeTeal,
    onSecondary = Color.White,
    secondaryContainer = WazeTealLight,
    onSecondaryContainer = WazeTextPrimary,
    background = WazeWarmWhite,
    onBackground = WazeTextPrimary,
    surface = WazeCardBg,
    onSurface = WazeTextPrimary,
    error = WazeRed,
    onError = Color.White,
    errorContainer = WazeRedLight,
    onErrorContainer = WazeRed,
    outline = WazeBorder
)

@Composable
fun KidSeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Waze signature style preserves bright, warm look
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = WazeWarmWhite.toArgb()
                window.navigationBarColor = WazeWarmWhite.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KidSeeTypography,
        content = content
    )
}
