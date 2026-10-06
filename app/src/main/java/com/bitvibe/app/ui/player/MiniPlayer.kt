package com.bitvibe.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bitvibe.app.domain.player.MusicController
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey

@Composable
fun MiniPlayer(
    musicController: MusicController,
    onExpand: () -> Unit
) {
    val currentSong by musicController.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicController.isPlaying.collectAsStateWithLifecycle()
    val position by musicController.currentPosition.collectAsStateWithLifecycle()

    if (currentSong == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onExpand)
    ) {
        // Progress bar at top
        val duration = (currentSong?.duration ?: 1L).coerceAtLeast(1L)
        LinearProgressIndicator(
            progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = BitVibeCyan,
            trackColor = Color.White.copy(alpha = 0.1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art thumbnail
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder underneath; covered when the album art loads.
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = TextGrey,
                    modifier = Modifier.size(20.dp)
                )
                if (currentSong?.albumArtUri != null) {
                    AsyncImage(
                        model = currentSong?.albumArtUri,
                        contentDescription = currentSong?.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentSong?.title ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentSong?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Play/Pause
            IconButton(
                onClick = { musicController.togglePlayPause() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Skip next
            IconButton(
                onClick = { musicController.skipToNext() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
