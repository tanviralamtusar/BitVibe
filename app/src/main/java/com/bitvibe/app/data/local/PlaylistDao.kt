package com.bitvibe.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.bitvibe.app.data.model.Playlist
import com.bitvibe.app.data.model.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistById(playlistId: Long): Flow<Playlist?>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSong(song: PlaylistSong): Long

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND audioId = :audioId")
    suspend fun removeSongFromPlaylist(playlistId: Long, audioId: Long)
    
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongCount(playlistId: Long): Int
    
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId AND audioId = :audioId")
    suspend fun countSong(playlistId: Long, audioId: Long): Int

    @Query("SELECT MAX(orderIndex) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getMaxOrderIndex(playlistId: Long): Int?

    @Query("UPDATE playlists SET songCount = :count WHERE id = :playlistId")
    suspend fun updateSongCount(playlistId: Long, count: Int)
}
