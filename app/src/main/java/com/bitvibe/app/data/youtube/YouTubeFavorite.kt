package com.bitvibe.app.data.youtube

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/** A saved YouTube video: only its id and display info, never any media. */
@Entity(tableName = "youtube_favorites")
data class YouTubeFavorite(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val channelId: String,
    val thumbnailUrl: String,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toVideo() = YouTubeVideo(videoId, title, channelTitle, channelId, thumbnailUrl)
}

fun YouTubeVideo.toFavorite() = YouTubeFavorite(id, title, channelTitle, channelId, thumbnailUrl)

@Dao
interface YouTubeFavoriteDao {
    @Query("SELECT * FROM youtube_favorites ORDER BY addedAt DESC")
    fun getAll(): Flow<List<YouTubeFavorite>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: YouTubeFavorite)

    @Query("DELETE FROM youtube_favorites WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}

/** v1 → v2: adds the YouTube favorites table; playlists are untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `youtube_favorites` (" +
                "`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `channelTitle` TEXT NOT NULL, " +
                "`channelId` TEXT NOT NULL, `thumbnailUrl` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`videoId`))"
        )
    }
}
