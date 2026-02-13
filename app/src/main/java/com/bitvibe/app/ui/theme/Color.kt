package com.bitvibe.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Primary Accent ────────────────────────────────────────────
val BitVibeCyan = Color(0xFF1ED8B4)
val BitVibeCyanBright = Color(0xFF00E4C6)
val BitVibeCyanDark = Color(0xFF0FA88A)

// ── Backgrounds (AMOLED-friendly) ─────────────────────────────
val DarkBg = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1A1A1A)
val DarkSurfaceVariant = Color(0xFF242424)
val DarkSurfaceElevated = Color(0xFF2A2A2A)

// ── Text Colors ───────────────────────────────────────────────
val TextWhite = Color(0xFFFFFFFF)
val TextGrey = Color(0xFFB3B3B3)
val TextMuted = Color(0xFF727272)

// ── Utility ───────────────────────────────────────────────────
val DividerColor = Color(0xFF2A2A2A)
val OverlayBlack = Color(0x99000000)

// ── Gradient Brushes ──────────────────────────────────────────
val AccentGradient = Brush.horizontalGradient(listOf(BitVibeCyan, BitVibeCyanBright))
val HeaderGradient = Brush.verticalGradient(
    listOf(BitVibeCyan.copy(alpha = 0.15f), Color.Transparent)
)
val DialogGradient = Brush.verticalGradient(
    listOf(Color.White.copy(alpha = 0.4f), Color.Black.copy(alpha = 0.9f))
)