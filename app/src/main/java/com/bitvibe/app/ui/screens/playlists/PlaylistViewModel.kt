package com.bitvibe.app.ui.screens.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.data.model.Playlist
import com.bitvibe.app.data.model.PlaylistSong
import com.bitvibe.app.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val repository: PlaylistRepository
) : ViewModel() {

    val playlists = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun playlist(playlistId: Long): Flow<Playlist?> = repository.getPlaylist(playlistId)

    fun songs(playlistId: Long): Flow<List<PlaylistSong>> = repository.getSongsForPlaylist(playlistId)

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }

    /** Creates a playlist and immediately adds [audio] to it. */
    fun createPlaylistWithSong(name: String, audio: AudioFile, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            onDone(repository.addSongToPlaylist(id, audio))
        }
    }

    /** [onDone] receives false when the song was already in the playlist. */
    fun addSongToPlaylist(playlistId: Long, audio: AudioFile, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onDone(repository.addSongToPlaylist(playlistId, audio))
        }
    }

    fun removeSong(playlistId: Long, audioId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, audioId)
        }
    }

    fun deletePlaylist(playlist: Playlist, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            onDone()
        }
    }
}
