package com.bitvibe.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.data.model.Playlist
import com.bitvibe.app.ui.screens.playlists.PlaylistViewModel
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted
import java.io.File

private const val FILTER_PLAYLISTS = "Playlists"
private const val FILTER_ARTISTS = "Artists"
private const val FILTER_ALBUMS = "Albums"
private const val FILTER_FOLDERS = "Folders"

/** A set of songs shown as one row under the Artists / Albums / Folders filters. */
private data class SongGroup(
    val key: String,
    val title: String,
    val subtitle: String,
    val songs: List<AudioFile>
)

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    onPlaylistClick: (Long) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
    libraryViewModel: LibraryViewModel = hiltViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val audioFiles by libraryViewModel.audioFiles.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(FILTER_PLAYLISTS) }
    var expandedGroup by rememberSaveable { mutableStateOf<String?>(null) }

    val filters = listOf(FILTER_PLAYLISTS, FILTER_ARTISTS, FILTER_ALBUMS, FILTER_FOLDERS)

    val groups = remember(audioFiles, selectedFilter) {
        when (selectedFilter) {
            FILTER_ARTISTS -> audioFiles
                .groupBy { it.artist }
                .map { (artist, songs) -> SongGroup(artist, artist, songCountLabel(songs.size), songs) }
                .sortedBy { it.title.lowercase() }
            FILTER_ALBUMS -> audioFiles
                .filter { it.album.isNotBlank() }
                .groupBy { it.album }
                .map { (album, songs) ->
                    val artist = songs.map { it.artist }.distinct().singleOrNull() ?: "Various artists"
                    SongGroup(album, album, "$artist · ${songCountLabel(songs.size)}", songs)
                }
                .sortedBy { it.title.lowercase() }
            FILTER_FOLDERS -> audioFiles
                .groupBy { File(it.path).parent.orEmpty() }
                .map { (folder, songs) ->
                    SongGroup(
                        key = folder,
                        title = File(folder).name.ifBlank { "Internal storage" },
                        subtitle = songCountLabel(songs.size),
                        songs = songs
                    )
                }
                .sortedBy { it.title.lowercase() }
            else -> emptyList()
        }
    }

    val groupIcon = when (selectedFilter) {
        FILTER_ARTISTS -> Icons.Outlined.Person
        FILTER_ALBUMS -> Icons.Outlined.Album
        else -> Icons.Outlined.Folder
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = BitVibeCyan,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Your Library",
                style = MaterialTheme.typography.headlineLarge,
                color = BitVibeCyan
            )
        }

        // ── Filter Chips ────────────────────────────────
        LazyRow(
            modifier = Modifier.padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = {
                        selectedFilter = filter
                        expandedGroup = null
                    },
                    label = {
                        Text(
                            text = filter,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.Transparent,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        selectedContainerColor = BitVibeCyan,
                        selectedLabelColor = Color.Black
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        selectedBorderColor = BitVibeCyan,
                        enabled = true,
                        selected = selectedFilter == filter
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // ── Content ─────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (selectedFilter == FILTER_PLAYLISTS) {
                // Add New Playlist button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateDialog = true }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(BitVibeCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Add New Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (playlists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your playlists",
                            style = MaterialTheme.typography.titleMedium,
                            color = BitVibeCyan,
                            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
                        )
                    }

                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistItem(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.id) }
                        )
                    }
                } else {
                    item {
                        LibraryEmptyText("No playlists yet. Create one, then add songs from the player with +.")
                    }
                }
            } else {
                if (groups.isEmpty()) {
                    item { LibraryEmptyText("No music found on this device.") }
                }
                groups.forEach { group ->
                    val expanded = expandedGroup == group.key
                    item(key = "group:${group.key}") {
                        SongGroupRow(
                            group = group,
                            icon = groupIcon,
                            expanded = expanded,
                            onClick = { expandedGroup = if (expanded) null else group.key },
                            onPlay = { libraryViewModel.playSong(group.songs.first(), group.songs) }
                        )
                    }
                    if (expanded) {
                        items(group.songs, key = { "song:${group.key}:${it.id}" }) { audio ->
                            GroupSongRow(
                                audio = audio,
                                onClick = { libraryViewModel.playSong(audio, group.songs) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Create Playlist Dialog ───────────────────────
    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            }
        )
    }
}

private fun songCountLabel(count: Int) = if (count == 1) "1 song" else "$count songs"

@Composable
private fun LibraryEmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextGrey,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
    )
}

@Composable
private fun SongGroupRow(
    group: SongGroup,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    onPlay: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TextGrey, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = group.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${group.title}", tint = BitVibeCyan)
        }
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = TextGrey
        )
    }
}

@Composable
private fun GroupSongRow(audio: AudioFile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 90.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = audio.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = audio.artist,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rounded square art placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = TextGrey,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                text = "New Playlist",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                placeholder = {
                    Text(
                        "Give your playlist a title",
                        color = TextMuted
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BitVibeCyan,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    cursorColor = BitVibeCyan
                ),
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { if (playlistName.isNotBlank()) onCreate(playlistName.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BitVibeCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
