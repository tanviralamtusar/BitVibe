package com.bitvibe.app.ui.player

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.ui.screens.playlists.PlaylistViewModel
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted

/** Lets the user add [song] to an existing playlist, or create a new one containing it. */
@Composable
fun AddToPlaylistDialog(
    song: AudioFile,
    onDismiss: () -> Unit,
    viewModel: PlaylistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    fun report(added: Boolean, playlistName: String) {
        val message = if (added) "Added to $playlistName" else "Already in $playlistName"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Text(
                text = if (creating) "New Playlist" else "Add to playlist",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            if (creating) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text("Give your playlist a title", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BitVibeCyan,
                        cursorColor = BitVibeCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item {
                        PlaylistChoiceRow(
                            icon = { Icon(Icons.Filled.Add, null, tint = BitVibeCyan) },
                            title = "New playlist",
                            onClick = { creating = true }
                        )
                    }
                    items(playlists, key = { it.id }) { playlist ->
                        PlaylistChoiceRow(
                            icon = { Icon(Icons.Outlined.QueueMusic, null, tint = TextGrey) },
                            title = playlist.name,
                            subtitle = "${playlist.songCount} songs",
                            onClick = {
                                viewModel.addSongToPlaylist(playlist.id, song) { added ->
                                    report(added, playlist.name)
                                }
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            viewModel.createPlaylistWithSong(name, song) { added -> report(added, name) }
                            onDismiss()
                        }
                    },
                    enabled = newName.isNotBlank()
                ) { Text("Create", color = BitVibeCyan) }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (creating) creating = false else onDismiss() }) {
                Text(if (creating) "Back" else "Cancel", color = TextGrey)
            }
        }
    )
}

@Composable
private fun PlaylistChoiceRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextGrey)
            }
        }
    }
}
