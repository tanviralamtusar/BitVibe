package com.bitvibe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BitVibeColorScheme = darkColorScheme(
    primary = BitVibeCyan,
    onPrimary = Color.Black,
    primaryContainer = BitVibeCyanDark,
    onPrimaryContainer = BitVibeCyanBright,

    secondary = BitVibeCyan,
    onSecondary = Color.Black,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = TextWhite,

    tertiary = BitVibeCyanBright,
    onTertiary = Color.Black,

    background = DarkBg,
    onBackground = TextWhite,

    surface = DarkSurface,
    onSurface = TextWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextGrey,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = DarkSurfaceElevated,

    outline = DividerColor,
    outlineVariant = DividerColor,

    inverseSurface = TextWhite,
    inverseOnSurface = DarkBg,
    inversePrimary = BitVibeCyanDark,

    error = Color(0xFFCF6679),
    onError = Color.Black,
)

@Composable
fun BitVibeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BitVibeColorScheme,
        typography = BitVibeTypography,
        content = content
    )
}