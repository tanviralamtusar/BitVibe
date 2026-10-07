package com.bitvibe.app.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import com.bitvibe.app.data.art.AlbumArt
import com.bitvibe.app.data.art.ArtPalette
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.data.waveform.WaveformRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Per-song visuals for the player: wave seek levels and the cover's accent colour. */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val waveforms: WaveformRepository
) : ViewModel() {

    private val accentCache = mutableMapOf<Long, Int?>()

    suspend fun waveform(song: AudioFile): FloatArray =
        waveforms.waveform(song.id, song.contentUri, song.duration)

    /** ARGB accent from the album art, or null to use the app accent. */
    suspend fun accentColor(song: AudioFile): Int? {
        if (accentCache.containsKey(song.id)) return accentCache[song.id]
        val art = song.albumArtUri?.takeIf { AlbumArt.isArtUri(it) } ?: return null
        val color = withContext(Dispatchers.IO) {
            AlbumArt.load(context, art, 96)?.let { ArtPalette.accentFrom(it) }
        }
        accentCache[song.id] = color
        return color
    }
}
