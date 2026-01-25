package com.bitvibe.app.domain.repository

import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.data.model.Folder
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllAudio(): Flow<List<AudioFile>>
    fun getAudioByFolder(folderPath: String): Flow<List<AudioFile>>
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun scanMedia()
}
