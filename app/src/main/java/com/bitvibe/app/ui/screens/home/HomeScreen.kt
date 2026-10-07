package com.bitvibe.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.ui.screens.library.LibraryViewModel
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.BitVibeCyanDark
import com.bitvibe.app.ui.theme.DarkSurfaceVariant
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted

// Vibrant accent colors for mix cards
private val MixColors = listOf(
    Color(0xFFE85D75),  // Pink/Red
    Color(0xFF1ED8B4),  // Cyan
    Color(0xFFB24EF0),  // Purple
    Color(0xFF4EBF5A),  // Green
    Color(0xFFE8A317),  // Amber
    Color(0xFF4A90D9),  // Blue
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val audioFiles by viewModel.audioFiles.collectAsStateWithLifecycle()

    // Create "Continue Listening" items (first 6 songs)
    val continueListening = remember(audioFiles) { audioFiles.take(6) }

    // Create "Top Mixes" by grouping by artist
    val topMixes = remember(audioFiles) {
        audioFiles
            .groupBy { it.artist }
            .entries
            // Artists with the most songs make the most interesting mixes.
            .sortedByDescending { it.value.size }
            .take(6)
            .mapIndexed { index, entry ->
                MixData(
                    title = if (entry.key == "Unknown artist") "Mix ${index + 1}" else "${entry.key} Mix",
                    songs = entry.value,
                    color = MixColors[index % MixColors.size]
                )
            }
    }

    // "Based on recent listening" (last songs in the list)
    val recentListening = remember(audioFiles) {
        audioFiles.takeLast(6.coerceAtMost(audioFiles.size)).reversed()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Welcome Header ──────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = null,
                        tint = TextGrey,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (audioFiles.isEmpty()) "BeatVibe Player" else "${audioFiles.size} songs on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGrey
                    )
                }

                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = TextGrey,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── Continue Listening ──────────────────────────
        if (continueListening.isNotEmpty()) {
            item {
                Text(
                    text = "Continue Listening",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )
            }

            // 2-column grid (pairs of songs)
            val rows = continueListening.chunked(2)
            items(rows) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { audio ->
                        ContinueListeningCard(
                            audio = audio,
                            onClick = { viewModel.playSong(audio, continueListening) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // If odd number, fill remaining space
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Your Top Mixes ──────────────────────────────
        if (topMixes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Your Top Mixes",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topMixes) { mix ->
                        TopMixCard(
                            mix = mix,
                            onClick = {
                                mix.songs.firstOrNull()?.let { viewModel.playSong(it, mix.songs) }
                            }
                        )
                    }
                }
            }
        }

        // ── Based on your recent listening ──────────────
        if (recentListening.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = "Based on your recent listening",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentListening) { audio ->
                        RecentListeningCard(
                            audio = audio,
                            onClick = { viewModel.playSong(audio, recentListening) }
                        )
                    }
                }
            }
        }

        // ── Empty State ─────────────────────────────────
        if (audioFiles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.MusicNote,
                            contentDescription = null,
                            tint = TextGrey,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No songs found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextGrey
                        )
                        Text(
                            text = "Add music to your device to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

// ── Continue Listening Card (compact row card) ──────────
@Composable
private fun ContinueListeningCard(
    audio: AudioFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Square album art
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder first; album art (when the file has any) draws over it.
                Icon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )

                if (audio.albumArtUri != null) {
                    AsyncImage(
                        model = audio.albumArtUri,
                        contentDescription = audio.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Text(
                text = audio.title.replace("_", " "),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
        }
    }
}

// ── Top Mix Card (large card with overlay) ──────────────
@Composable
private fun TopMixCard(
    mix: MixData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val firstSong = mix.songs.firstOrNull()
            // Placeholder first; album art (when the file has any) draws over it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                mix.color.copy(alpha = 0.7f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )
            Icon(
                Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.3f),
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
            )

            if (firstSong?.albumArtUri != null) {
                AsyncImage(
                    model = firstSong.albumArtUri,
                    contentDescription = mix.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Dark gradient overlay at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Title overlay
            Text(
                text = mix.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 28.dp, end = 12.dp)
            )

            // Colored progress bar at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .background(mix.color)
            )
        }
    }
}

// ── Recent Listening Card (large square) ────────────────
@Composable
private fun RecentListeningCard(
    audio: AudioFile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column {
            // Album art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder first; album art (when the file has any) draws over it.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    BitVibeCyanDark.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
                Icon(
                    Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(36.dp)
                )

                if (audio.albumArtUri != null) {
                    AsyncImage(
                        model = audio.albumArtUri,
                        contentDescription = audio.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Title
            Text(
                text = audio.title.replace("_", " "),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

// ── Data class for mixes ────────────────────────────────
private data class MixData(
    val title: String,
    val songs: List<AudioFile>,
    val color: Color
)
