package com.bitvibe.app.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun VisualizerView(
    waveform: ByteArray,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        if (waveform.isEmpty()) return@Canvas

        val width = size.width
        val height = size.height
        val centerY = height / 2f
        
        // Android Visualizer returns unsigned bytes as signed bytes (-128 to 127).
        // 0 (128 unsigned) is silence.
        // We downsample to fit screen width reasonably.
        
        val points = mutableListOf<Offset>()
        val step = 4 // Skip some points for performance
        
        for (i in 0 until waveform.size step step) {
            val x = (i.toFloat() / waveform.size) * width
            // Convert byte (-128..127) to 0..255, then normalize around 128
            val byte = waveform[i].toInt() + 128
            val normalized = (byte - 128) / 128f // -1.0 to 1.0
            val y = centerY + (normalized * (height / 2f))
            
            points.add(Offset(x, y))
        }

        // Draw lines
        if (points.isNotEmpty()) {
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = color,
                    start = points[i],
                    end = points[i+1],
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
