package com.bitvibe.app.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "playlist_songs",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId")]
)
data class PlaylistSong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: Long,
    val audioId: Long, // References MediaStore ID
    val title: String, // Cached metadata
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String, // Persist path? URI is better but path works for scan matching
    val contentUriString: String, 
    val albumArtUriString: String,
    val orderIndex: Int = 0
)

fun PlaylistSong.toAudioFile(): AudioFile = AudioFile(
    id = audioId,
    title = title,
    artist = artist,
    album = album,
    duration = duration,
    path = path,
    albumArtUri = albumArtUriString.takeIf { it.isNotBlank() }?.let(Uri::parse),
    contentUri = Uri.parse(contentUriString)
)
