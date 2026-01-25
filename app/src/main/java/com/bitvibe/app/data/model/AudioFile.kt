package com.bitvibe.app.data.model

import android.net.Uri

data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val albumArtUri: Uri? = null,
    val contentUri: Uri
)
