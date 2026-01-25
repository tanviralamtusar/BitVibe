package com.bitvibe.app.data.repository

import com.bitvibe.app.data.local.PlaylistDao
import com.bitvibe.app.data.model.Playlist
import com.bitvibe.app.data.model.PlaylistSong
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSong>> = playlistDao.getSongsForPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun addSongToPlaylist(playlistId: Long, audioId: Long, title: String, artist: String, album: String, duration: Long, path: String, contentUriString: String, albumArtUriString: String) {
        val currentCount = playlistDao.getSongCount(playlistId)
        val song = PlaylistSong(
            playlistId = playlistId,
            audioId = audioId,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            path = path,
            contentUriString = contentUriString,
            albumArtUriString = albumArtUriString,
            orderIndex = currentCount
        )
        playlistDao.insertPlaylistSong(song)
        playlistDao.updateSongCount(playlistId, currentCount + 1)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, audioId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, audioId)
        // Note: Re-indexing logic would be needed for perfect order, skipping for MVP.
        val currentCount = playlistDao.getSongCount(playlistId)
        playlistDao.updateSongCount(playlistId, currentCount) // Count will be auto updated by generic logic? No, explicit update needed if we rely on it.
    }
    
    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }
}
