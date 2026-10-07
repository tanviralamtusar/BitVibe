package com.bitvibe.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitvibe.app.domain.player.EqBand
import com.bitvibe.app.domain.player.MusicController
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted

enum class PlayerTool(val label: String) { Loop("A-B Loop"), Speed("Speed"), Equalizer("Equalizer") }

private val SPEED_PRESETS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
private const val NUDGE_MS = 100L

/** BeatVibe's practice tools in one sheet, opened from the player's action row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerToolsSheet(
    musicController: MusicController,
    tool: PlayerTool,
    accent: Color,
    onToolChange: (PlayerTool) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PlayerTool.entries.forEach { t ->
                    PillButton(
                        text = t.label,
                        active = t == tool,
                        accent = accent,
                        modifier = Modifier.weight(1f),
                        onClick = { onToolChange(t) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            when (tool) {
                PlayerTool.Loop -> LoopTool(musicController, accent)
                PlayerTool.Speed -> SpeedTool(musicController, accent)
                PlayerTool.Equalizer -> EqualizerTool(musicController, accent)
            }
        }
    }
}

@Composable
private fun LoopTool(musicController: MusicController, accent: Color) {
    val position by musicController.currentPosition.collectAsStateWithLifecycle()
    val loopStart by musicController.loopStart.collectAsStateWithLifecycle()
    val loopEnd by musicController.loopEnd.collectAsStateWithLifecycle()
    val loopMode by musicController.loopMode.collectAsStateWithLifecycle()

    Text(
        "Mark A and B while the song plays, then fine-tune each point by 0.1 s.",
        style = MaterialTheme.typography.bodySmall,
        color = TextGrey
    )
    Spacer(modifier = Modifier.height(16.dp))
    LoopPointRow(
        label = "A",
        valueMs = loopStart,
        accent = accent,
        onSet = { musicController.setLoopStart(position) },
        onNudge = { delta -> loopStart?.let { musicController.setLoopStart((it + delta).coerceAtLeast(0L)) } }
    )
    Spacer(modifier = Modifier.height(10.dp))
    LoopPointRow(
        label = "B",
        valueMs = loopEnd,
        accent = accent,
        onSet = { musicController.setLoopEnd(position) },
        onNudge = { delta -> loopEnd?.let { musicController.setLoopEnd(it + delta) } }
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        PillButton(
            text = if (loopMode) "Looping" else "Loop off",
            active = loopMode,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { musicController.toggleLoopMode() }
        )
        PillButton(
            text = "Clear",
            active = false,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = { musicController.clearLoop() }
        )
    }
}

@Composable
private fun LoopPointRow(
    label: String,
    valueMs: Long?,
    accent: Color,
    onSet: () -> Unit,
    onNudge: (Long) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        PillButton(
            text = if (valueMs != null) "$label  ${formatPrecise(valueMs)}" else "Set $label",
            active = valueMs != null,
            accent = accent,
            modifier = Modifier.weight(1f),
            onClick = onSet
        )
        Spacer(modifier = Modifier.width(10.dp))
        RoundIconButton(Icons.Filled.ChevronLeft, "Move $label back 0.1 seconds", accent, enabled = valueMs != null) {
            onNudge(-NUDGE_MS)
        }
        Spacer(modifier = Modifier.width(8.dp))
        RoundIconButton(Icons.Filled.ChevronRight, "Move $label forward 0.1 seconds", accent, enabled = valueMs != null) {
            onNudge(NUDGE_MS)
        }
    }
}

@Composable
private fun SpeedTool(musicController: MusicController, accent: Color) {
    val speed by musicController.playbackSpeed.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RoundIconButton(Icons.Filled.Remove, "Slower", accent, size = 48) { musicController.setPlaybackSpeed(speed - 0.05f) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "%.2fx".format(speed),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (speed == 1f) MaterialTheme.colorScheme.onSurface else accent
            )
            Text("Pitch stays the same", style = MaterialTheme.typography.bodySmall, color = TextGrey)
        }
        RoundIconButton(Icons.Filled.Add, "Faster", accent, size = 48) { musicController.setPlaybackSpeed(speed + 0.05f) }
    }
    Spacer(modifier = Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        SPEED_PRESETS.forEach { preset ->
            PillButton(
                text = if (preset == 1f) "1x" else "${preset}x".replace(".0x", "x"),
                active = speed == preset,
                accent = accent,
                modifier = Modifier.weight(1f),
                onClick = { musicController.setPlaybackSpeed(preset) }
            )
        }
    }
}

@Composable
private fun EqualizerTool(musicController: MusicController, accent: Color) {
    val bands by musicController.equalizerBands.collectAsStateWithLifecycle()
    if (bands.isEmpty()) {
        Text(
            "The equalizer becomes available once a song starts playing.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGrey,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bands.forEach { band ->
            EqBandSlider(band = band, accent = accent) { level -> musicController.setBandLevel(band.id, level) }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        fun applyPreset(levelFor: (Int, EqBand) -> Int) =
            bands.forEachIndexed { i, b -> musicController.setBandLevel(b.id, levelFor(i, b).coerceIn(b.minLevel, b.maxLevel)) }
        val last = bands.size - 1
        val mid = bands.size / 2
        PillButton("Flat", false, accent, Modifier.weight(1f)) { applyPreset { _, _ -> 0 } }
        PillButton("Bass+", false, accent, Modifier.weight(1f)) {
            applyPreset { i, b -> when (i) { 0 -> (b.maxLevel * 0.7).toInt(); 1 -> (b.maxLevel * 0.5).toInt(); else -> 0 } }
        }
        PillButton("Vocal", false, accent, Modifier.weight(1f)) {
            applyPreset { i, b -> if (i in (mid - 1)..(mid + 1)) (b.maxLevel * 0.4).toInt() else (b.minLevel * 0.2).toInt() }
        }
        PillButton("Rock", false, accent, Modifier.weight(1f)) {
            applyPreset { i, b ->
                when (i) {
                    0 -> (b.maxLevel * 0.5).toInt(); 1 -> (b.maxLevel * 0.3).toInt()
                    last -> (b.maxLevel * 0.6).toInt(); last - 1 -> (b.maxLevel * 0.4).toInt()
                    else -> 0
                }
            }
        }
    }
}

/** Vertical band slider: drag or tap along the track; the label shows the gain in dB. */
@Composable
private fun EqBandSlider(band: EqBand, accent: Color, onValueChange: (Int) -> Unit) {
    val range = (band.maxLevel - band.minLevel).coerceAtLeast(1)
    val fraction = (band.currentLevel - band.minLevel).toFloat() / range
    var trackHeight by remember { mutableFloatStateOf(1f) }
    fun levelAt(y: Float): Int {
        val f = (1f - y / trackHeight).coerceIn(0f, 1f)
        return band.minLevel + (f * range).toInt()
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(52.dp)) {
        Text(
            "%+d".format(band.currentLevel / 100),
            style = MaterialTheme.typography.labelSmall,
            color = if (band.currentLevel != 0) accent else TextMuted
        )
        Spacer(modifier = Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .weight(1f)
                .width(44.dp)
                .pointerInput(band.id, band.minLevel, band.maxLevel) {
                    trackHeight = size.height.toFloat()
                    detectTapGestures { onValueChange(levelAt(it.y)) }
                }
                .pointerInput(band.id, band.minLevel, band.maxLevel) {
                    trackHeight = size.height.toFloat()
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        onValueChange(levelAt(change.position.y))
                    }
                }
        ) {
            val cx = size.width / 2
            val stroke = 4.dp.toPx()
            val zeroY = size.height * (1f - (0 - band.minLevel).toFloat() / range)
            val thumbY = size.height * (1f - fraction)
            drawLine(accent.copy(alpha = 0.18f), Offset(cx, 0f), Offset(cx, size.height), stroke, StrokeCap.Round)
            drawLine(accent, Offset(cx, zeroY), Offset(cx, thumbY), stroke, StrokeCap.Round)
            drawLine(accent.copy(alpha = 0.5f), Offset(cx - 8.dp.toPx(), zeroY), Offset(cx + 8.dp.toPx(), zeroY), 1.dp.toPx())
            drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(cx, thumbY))
            drawCircle(accent, radius = 5.dp.toPx(), center = Offset(cx, thumbY))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            band.name.replace(" Hz", "").let { val f = it.toIntOrNull() ?: 0; if (f >= 1000) "${f / 1000}k" else it },
            style = MaterialTheme.typography.labelSmall,
            color = TextGrey,
            maxLines = 1
        )
    }
}

@Composable
fun PillButton(
    text: String,
    active: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) accent else Color.Transparent)
            .border(1.dp, if (active) accent else accent.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (active) Color.Black else accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    description: String,
    accent: Color,
    enabled: Boolean = true,
    size: Int = 40,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(1.dp, accent.copy(alpha = if (enabled) 0.6f else 0.2f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else TextMuted,
            modifier = Modifier.size((size / 2).dp)
        )
    }
}

/** m:ss.t — tenths, so 0.1 s nudges are visible. */
private fun formatPrecise(ms: Long): String {
    val tenths = (ms / 100) % 10
    return "${formatTime(ms)}.$tenths"
}
