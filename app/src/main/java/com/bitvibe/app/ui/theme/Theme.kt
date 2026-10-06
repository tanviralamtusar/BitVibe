package com.bitvibe.app.ui.theme

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

// Pure-black variant for OLED screens: true black background, slightly lifted surfaces.
private val BitVibeAmoledColorScheme = BitVibeColorScheme.copy(
    background = Color.Black,
    surface = Color(0xFF101010),
    surfaceVariant = Color(0xFF181818),
    surfaceContainer = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    inverseOnSurface = Color.Black,
)

@Composable
fun BitVibeTheme(
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (amoled) BitVibeAmoledColorScheme else BitVibeColorScheme,
        typography = BitVibeTypography,
        content = content
    )
}
