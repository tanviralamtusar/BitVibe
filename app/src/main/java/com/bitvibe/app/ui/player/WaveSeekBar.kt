package com.bitvibe.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bitvibe.app.ui.theme.TextGrey

/**
 * Poweramp-style "waveseek": the track's loudness as bars, played part in [accent].
 * Tap or drag anywhere to seek (the seek happens on release, so dragging doesn't stutter).
 * The A-B segment is shaded and labelled; stronger while the loop is on.
 */
@Composable
fun WaveSeekBar(
    levels: FloatArray?,
    positionMs: Long,
    durationMs: Long,
    loopStartMs: Long?,
    loopEndMs: Long?,
    loopActive: Boolean,
    accent: Color,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(1L)
    var scrub by remember { mutableFloatStateOf(-1f) }
    val isScrubbing = scrub >= 0f
    val progress = if (isScrubbing) scrub else (positionMs.toFloat() / duration).coerceIn(0f, 1f)
    val shownPosition = if (isScrubbing) (scrub * duration).toLong() else positionMs
    val textMeasurer = rememberTextMeasurer()
    val idleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
    val labelStyle = TextStyle(color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = "Seek bar, ${formatTime(shownPosition)} of ${formatTime(durationMs)}" }
                .pointerInput(enabled, duration) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        onSeek(((offset.x / size.width).coerceIn(0f, 1f) * duration).toLong())
                    }
                }
                .pointerInput(enabled, duration) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = { offset -> scrub = (offset.x / size.width).coerceIn(0f, 1f) },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            scrub = (change.position.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            if (scrub >= 0f) onSeek((scrub * duration).toLong())
                            scrub = -1f
                        },
                        onDragCancel = { scrub = -1f }
                    )
                }
        ) {
            val labelSpace = 14.dp.toPx()
            val barArea = size.height - labelSpace
            val count = levels?.size ?: 96
            val slot = size.width / count
            val barWidth = (slot * 0.62f).coerceAtLeast(1f)
            val minBar = 2.dp.toPx()

            // A-B segment
            val a = loopStartMs?.let { (it.toFloat() / duration).coerceIn(0f, 1f) * size.width }
            val b = loopEndMs?.let { (it.toFloat() / duration).coerceIn(0f, 1f) * size.width }
            if (a != null && b != null && b > a) {
                drawRect(
                    color = accent.copy(alpha = if (loopActive) 0.22f else 0.1f),
                    topLeft = Offset(a, labelSpace),
                    size = Size(b - a, barArea)
                )
            }
            listOfNotNull(a?.let { it to "A" }, b?.let { it to "B" }).forEach { (x, label) ->
                drawLine(accent, Offset(x, labelSpace), Offset(x, size.height), strokeWidth = 1.5.dp.toPx())
                val text = textMeasurer.measure(label, labelStyle)
                drawText(text, topLeft = Offset(x - text.size.width / 2f, 0f))
            }

            // Bars, mirrored around the middle of the bar area
            val centerY = labelSpace + barArea / 2f
            for (i in 0 until count) {
                val level = levels?.get(i) ?: 0.18f
                val h = (level * barArea * 0.92f).coerceAtLeast(minBar)
                val x = i * slot + (slot - barWidth) / 2f
                val played = (i + 0.5f) / count <= progress
                drawRoundRect(
                    color = if (played) accent else idleColor,
                    topLeft = Offset(x, centerY - h / 2f),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }

            // Playhead
            val headX = progress * size.width
            drawLine(
                Color.White,
                Offset(headX, labelSpace),
                Offset(headX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatTime(shownPosition),
                style = MaterialTheme.typography.bodySmall,
                color = if (isScrubbing) accent else TextGrey
            )
            Text(formatTime(durationMs), style = MaterialTheme.typography.bodySmall, color = TextGrey)
        }
    }
}

/** m:ss, or h:mm:ss for long tracks. */
fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
