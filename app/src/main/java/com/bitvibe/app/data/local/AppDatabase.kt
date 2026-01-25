package com.bitvibe.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bitvibe.app.data.model.Playlist
import com.bitvibe.app.data.model.PlaylistSong

@Database(entities = [Playlist::class, PlaylistSong::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
}
