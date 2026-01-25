package com.bitvibe.app.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.data.model.Folder
import com.bitvibe.app.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class MediaRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaRepository {

    private val _audioFiles = MutableStateFlow<List<AudioFile>>(emptyList())
    private val _folders = MutableStateFlow<List<Folder>>(emptyList())

    override fun getAllAudio(): Flow<List<AudioFile>> = _audioFiles.asStateFlow()

    override fun getAudioByFolder(folderPath: String): Flow<List<AudioFile>> {
        return _audioFiles.map { list ->
            list.filter { File(it.path).parent == folderPath }
        }
    }


    override fun getAllFolders(): Flow<List<Folder>> = _folders.asStateFlow()

    override suspend fun scanMedia() = withContext(Dispatchers.IO) {
        val audioList = mutableListOf<AudioFile>()
        val folderMap = mutableMapOf<String, Int>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000" // > 10s
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                    val artist = cursor.getString(artistColumn)
                    val album = cursor.getString(albumColumn)
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(pathColumn)
                    val albumId = cursor.getLong(albumIdColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                    val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)

                    val audioFile = AudioFile(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        path = path,
                        albumArtUri = albumArtUri,
                        contentUri = contentUri
                    )
                    audioList.add(audioFile)

                    // Folder logic
                    val parentFile = File(path).parentFile
                    if (parentFile != null) {
                        val folderPath = parentFile.absolutePath
                        val currentCount = folderMap.getOrDefault(folderPath, 0)
                        folderMap[folderPath] = currentCount + 1
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error scanning media", e)
        }

        val folders = folderMap.map { (path, count) ->
            Folder(name = File(path).name, path = path, songCount = count)
        }

        _audioFiles.value = audioList
        _folders.value = folders
    }
}
