package com.bitvibe.app.data.repository

import com.bitvibe.app.data.local.PlaylistDao
import com.bitvibe.app.data.model.AudioFile
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

    fun getPlaylist(playlistId: Long): Flow<Playlist?> = playlistDao.getPlaylistById(playlistId)

    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSong>> = playlistDao.getSongsForPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    /** Adds [audio] to the end of the playlist. Returns false if it was already there. */
    suspend fun addSongToPlaylist(playlistId: Long, audio: AudioFile): Boolean {
        if (playlistDao.countSong(playlistId, audio.id) > 0) return false
        val song = PlaylistSong(
            playlistId = playlistId,
            audioId = audio.id,
            title = audio.title,
            artist = audio.artist,
            album = audio.album,
            duration = audio.duration,
            path = audio.path,
            contentUriString = audio.contentUri.toString(),
            albumArtUriString = audio.albumArtUri?.toString() ?: "",
            orderIndex = (playlistDao.getMaxOrderIndex(playlistId) ?: -1) + 1
        )
        playlistDao.insertPlaylistSong(song)
        playlistDao.updateSongCount(playlistId, playlistDao.getSongCount(playlistId))
        return true
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, audioId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, audioId)
        playlistDao.updateSongCount(playlistId, playlistDao.getSongCount(playlistId))
    }
    
    suspend fun deletePlaylist(playlist: Playlist) {
        playlistDao.deletePlaylist(playlist)
    }
}
