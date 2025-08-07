package com.hereliesaz.geministrator.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ExpressivePrimaryDark,
    onPrimary = ExpressiveOnPrimaryDark,
    secondary = ExpressiveSecondaryDark,
    onSecondary = ExpressiveOnSecondaryDark,
    tertiary = ExpressiveTertiaryDark,
    onTertiary = ExpressiveOnTertiaryDark,
    error = ExpressiveErrorDark,
    background = ExpressiveBackgroundDark,
    onBackground = ExpressiveOnBackgroundDark,
    surface = ExpressiveSurfaceDark,
    onSurface = ExpressiveOnSurfaceDark,
    surfaceVariant = ExpressiveSurfaceVariantDark,
    onSurfaceVariant = ExpressiveOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = ExpressivePrimaryLight,
    onPrimary = ExpressiveOnPrimaryLight,
    secondary = ExpressiveSecondaryLight,
    onSecondary = ExpressiveOnSecondaryLight,
    tertiary = ExpressiveTertiaryLight,
    onTertiary = ExpressiveOnTertiaryLight,
    error = ExpressiveErrorLight,
    background = ExpressiveBackgroundLight,
    onBackground = ExpressiveOnBackgroundLight,
    surface = ExpressiveSurfaceLight,
    onSurface = ExpressiveOnSurfaceLight,
    surfaceVariant = ExpressiveSurfaceVariantLight,
    onSurfaceVariant = ExpressiveOnBackgroundLight
)

@Composable
fun GeministratorTheme(
    themePreference: String = "System",
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themePreference) {
        "Light" -> false
        "Dark" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}