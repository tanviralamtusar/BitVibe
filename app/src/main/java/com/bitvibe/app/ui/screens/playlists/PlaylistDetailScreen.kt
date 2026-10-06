package com.bitvibe.app.ui.screens.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bitvibe.app.data.model.toAudioFile
import com.bitvibe.app.ui.screens.library.LibraryViewModel
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val playlist by remember(playlistId) { viewModel.playlist(playlistId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val songs by remember(playlistId) { viewModel.songs(playlistId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val queue = remember(songs) { songs.map { it.toAudioFile() } }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist?.name ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BitVibeCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGrey
                )
            }
            IconButton(onClick = { confirmDelete = true }, enabled = playlist != null) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete playlist", tint = TextGrey)
            }
        }

        if (queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp, start = 32.dp, end = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.MusicNote, null, tint = TextGrey, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("This playlist is empty", style = MaterialTheme.typography.bodyLarge, color = TextGrey)
                    Text(
                        "Open the player and tap + to add the current song",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            // Play all
            Button(
                onClick = { libraryViewModel.playSong(queue.first(), queue) },
                colors = ButtonDefaults.buttonColors(containerColor = BitVibeCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play all")
            }

            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                items(queue, key = { it.id }) { audio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { libraryViewModel.playSong(audio, queue) }
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.MusicNote, null, tint = TextGrey, modifier = Modifier.size(22.dp))
                            if (audio.albumArtUri != null) {
                                AsyncImage(
                                    model = audio.albumArtUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                audio.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                audio.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { viewModel.removeSong(playlistId, audio.id) }) {
                            Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = "Remove from playlist", tint = TextGrey)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Delete playlist?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "\"${playlist?.name.orEmpty()}\" will be removed. Your music files are not affected.",
                    color = TextGrey
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    // Navigate only after the delete finishes; leaving first would cancel it.
                    playlist?.let { viewModel.deletePlaylist(it, onDone = onBack) }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = BitVibeCyan) }
            }
        )
    }
}
