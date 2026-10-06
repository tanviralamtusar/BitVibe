package com.bitvibe.app.domain.player

import com.bitvibe.app.data.model.AudioFile
import kotlinx.coroutines.flow.StateFlow

interface MusicController {
    val isPlaying: StateFlow<Boolean>
    val currentSong: StateFlow<AudioFile?>

    /**
     * Plays [audioFile]. When [queue] is given (and contains [audioFile]) the whole list is
     * loaded so next/previous, shuffle and repeat-all work across it.
     */
    fun play(audioFile: AudioFile, queue: List<AudioFile> = listOf(audioFile))
    fun pause()
    fun resume()
    fun stop()
    fun togglePlayPause()

    // Speed Control
    val playbackSpeed: StateFlow<Float>
    fun setPlaybackSpeed(speed: Float)

    // A-B Loop
    val loopMode: StateFlow<Boolean>
    val loopStart: StateFlow<Long?>
    val loopEnd: StateFlow<Long?>
    val currentPosition: StateFlow<Long> // Exposed for UI progress/looping

    fun setLoopStart(ms: Long)
    fun setLoopEnd(ms: Long)
    fun clearLoop()
    fun toggleLoopMode()

    // Player Controls
    fun seekTo(position: Long)
    fun skipToNext()
    fun skipToPrevious()

    val shuffleModeEnabled: StateFlow<Boolean>
    val repeatMode: StateFlow<Int> // Player.REPEAT_MODE_OFF, ONE, ALL


    fun toggleShuffleMode()
    fun toggleRepeatMode()

    // Equalizer
    val equalizerBands: StateFlow<List<EqBand>>
    fun setBandLevel(bandId: Int, level: Int)

    // Visualizer
    val waveform: StateFlow<ByteArray>
}

data class EqBand(
    val id: Int,
    val name: String,
    val minLevel: Int,
    val maxLevel: Int,
    val currentLevel: Int
)
