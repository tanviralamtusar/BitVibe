package com.bitvibe.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bitvibe.app.data.model.Playlist
import com.bitvibe.app.data.model.PlaylistSong
import com.bitvibe.app.data.youtube.YouTubeFavorite
import com.bitvibe.app.data.youtube.YouTubeFavoriteDao

// v2 adds youtube_favorites (MIGRATION_1_2); keep migrations so existing playlists survive updates.
@Database(entities = [Playlist::class, PlaylistSong::class, YouTubeFavorite::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun youTubeFavoriteDao(): YouTubeFavoriteDao
}
