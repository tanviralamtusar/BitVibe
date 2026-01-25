package com.bitvibe.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Blue Theme Colors
private val PrimaryBlue = Color(0xFF64B5F6) // Light Blue 300 for Dark Mode contrast
private val SecondaryBlue = Color(0xFF90CAF9)
private val TertiaryBlue = Color(0xFF42A5F5)

private val DarkBackground = Color(0xFF121212) // Material Dark
private val SurfaceColor = Color(0xFF1E1E1E)
private val SurfaceContainer = Color(0xFF2C2C2C)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryBlue,
    tertiary = TertiaryBlue,
    background = DarkBackground,
    surface = SurfaceColor,
    surfaceContainer = SurfaceContainer,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

// Legacy Light (Optional fallback, but user requested consistent theme likely dark)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF42A5F5),
    tertiary = Color(0xFFBBDEFB)
)

@Composable
fun BitVibeTheme(
    darkTheme: Boolean = true, // Force Dark by default for "Clean" look
    content: @Composable () -> Unit
) {
    // User requested "sync entire app theme". Forcing our custom Dark Blue theme.
    // Ignoring dynamic color to ensure consistency.
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}