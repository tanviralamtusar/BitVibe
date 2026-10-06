package com.bitvibe.app.data.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.domain.player.MusicController
import com.bitvibe.app.player.service.MusicService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exoPlayer: ExoPlayer
) : MusicController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    private var equalizerInitialized = false

    // Songs in the current queue, keyed by MediaItem id, so metadata (art, URI) survives transitions.
    private val queuedSongs = mutableMapOf<String, AudioFile>()

    // A request made before the MediaController connected; replayed once it is ready.
    private var pendingAction: ((MediaController) -> Unit)? = null

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSong = MutableStateFlow<AudioFile?>(null)
    override val currentSong: StateFlow<AudioFile?> = _currentSong.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
        mediaControllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        mediaControllerFuture?.addListener({
            try {
                mediaController = mediaControllerFuture?.get()
                setupController()
            } catch (e: Exception) {
                Log.e("MusicController", "Error getting media controller", e)
            }
        }, MoreExecutors.directExecutor())
        
        // Also add listener directly to ExoPlayer for audio session
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && !equalizerInitialized) {
                    val sessionId = exoPlayer.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0) {
                        Log.d("MusicController", "Initializing EQ from ExoPlayer with session: $sessionId")
                        initEqualizer(sessionId)
                        initVisualizer(sessionId)
                        equalizerInitialized = true
                    }
                }
            }
        })
    }

    private fun setupController() {
        val controller = mediaController ?: return
        
        // Listener for events
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // A-B points belong to a single track; drop them when the track changes.
                if (mediaItem?.mediaId != _currentSong.value?.id?.toString()) {
                    clearLoop()
                }
                updateCurrentSong(mediaItem)
                _currentPosition.value = controller.currentPosition
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // MediaStore durations can be missing; fill in the real one once known.
                if (playbackState == Player.STATE_READY) {
                    val song = _currentSong.value ?: return
                    val duration = controller.duration
                    if (duration != C.TIME_UNSET && duration > 0 && duration != song.duration) {
                        _currentSong.value = song.copy(duration = duration)
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleModeEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
            
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    initEqualizer(audioSessionId)
                    initVisualizer(audioSessionId)
                    equalizerInitialized = true
                }
            }
        })
        
        // Initial state
        _isPlaying.value = controller.isPlaying
        _shuffleModeEnabled.value = controller.shuffleModeEnabled
        _repeatMode.value = controller.repeatMode
        updateCurrentSong(controller.currentMediaItem)
        
        // Try to init EQ if player is already ready
        val sessionId = exoPlayer.audioSessionId
        if (sessionId != C.AUDIO_SESSION_ID_UNSET && sessionId != 0 && !equalizerInitialized) {
            Log.d("MusicController", "Initializing EQ from setupController with session: $sessionId")
            initEqualizer(sessionId)
            initVisualizer(sessionId)
            equalizerInitialized = true
        }

        // Run a play request that arrived before the controller finished connecting.
        pendingAction?.invoke(controller)
        pendingAction = null
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            _currentSong.value = null
            return
        }

        // Prefer the AudioFile we queued ourselves; it carries album art and the real URI.
        val known = queuedSongs[mediaItem.mediaId]
        if (known != null) {
            _currentSong.value = known
            return
        }

        // Fallback (e.g. the session was restored without our queue): rebuild from metadata.
        val metadata = mediaItem.mediaMetadata
        val duration = metadata.extras?.getLong(EXTRA_DURATION)?.takeIf { it > 0 }
            ?: mediaController?.duration?.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: 0L

        _currentSong.value = AudioFile(
            id = mediaItem.mediaId.toLongOrNull() ?: 0L,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown",
            album = metadata.albumTitle?.toString() ?: "",
            duration = duration,
            path = "",
            albumArtUri = metadata.artworkUri,
            contentUri = mediaItem.localConfiguration?.uri ?: Uri.EMPTY
        )
    }

    private fun AudioFile.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(albumArtUri)
                    .setExtras(android.os.Bundle().apply {
                        putLong(EXTRA_DURATION, duration)
                    })
                    .build()
            )
            .build()

    override fun play(audioFile: AudioFile, queue: List<AudioFile>) {
        val items = queue.distinctBy { it.id }.takeIf { list -> list.any { it.id == audioFile.id } }
            ?: listOf(audioFile)
        val startIndex = items.indexOfFirst { it.id == audioFile.id }

        val controller = mediaController
        if (controller == null) {
            pendingAction = { play(audioFile, queue) }
            return
        }

        queuedSongs.clear()
        items.forEach { queuedSongs[it.id.toString()] = it }

        controller.setMediaItems(items.map { it.toMediaItem() }, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    override fun pause() {
        mediaController?.pause()
    }

    override fun resume() {
        mediaController?.play()
    }

    override fun stop() {
        mediaController?.stop()
    }

    override fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            // After the queue ended the player is idle/ended; restart it instead of doing nothing.
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekToDefaultPosition(0)
            } else if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        }
    }

    // Speed
    private val _playbackSpeed = MutableStateFlow(1f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    override fun setPlaybackSpeed(speed: Float) {
        // Round to avoid float drift (0.9000001x) from repeated +/- 0.1 steps.
        val clamped = (Math.round(speed * 100f) / 100f).coerceIn(MIN_SPEED, MAX_SPEED)
        mediaController?.playbackParameters = androidx.media3.common.PlaybackParameters(clamped, 1f)
        _playbackSpeed.value = clamped
    }

    // Loop
    private val _loopMode = MutableStateFlow(false)
    override val loopMode: StateFlow<Boolean> = _loopMode.asStateFlow()

    private val _loopStart = MutableStateFlow<Long?>(null)
    override val loopStart: StateFlow<Long?> = _loopStart.asStateFlow()

    private val _loopEnd = MutableStateFlow<Long?>(null)
    override val loopEnd: StateFlow<Long?> = _loopEnd.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    override val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    override fun setLoopStart(ms: Long) {
        _loopStart.value = ms
        // A new A at or after B invalidates the old segment.
        val end = _loopEnd.value
        if (end != null && end <= ms) {
            _loopEnd.value = null
            _loopMode.value = false
        }
    }

    override fun setLoopEnd(ms: Long) {
        val start = _loopStart.value ?: 0L
        if (ms <= start) return
        _loopStart.value = start
        _loopEnd.value = ms
        // Setting B completes the segment, so start looping right away.
        _loopMode.value = true
    }

    override fun clearLoop() {
        _loopStart.value = null
        _loopEnd.value = null
        _loopMode.value = false
    }

    override fun toggleLoopMode() {
        val enable = !_loopMode.value
        _loopMode.value = enable
        if (enable) {
            val start = _loopStart.value
            val end = _loopEnd.value
            val pos = _currentPosition.value
            if (start != null && end != null && (pos < start || pos >= end)) {
                seekTo(start)
            }
        }
    }
    
    // Polling for Position & Loop Check (Adaptive polling for efficiency)
    init {
        scope.launch {
            while (true) {
                val controller = mediaController
                val playing = controller?.isPlaying == true
                if (controller != null) {
                    val currentMs = controller.currentPosition
                    _currentPosition.value = currentMs
                    
                    // Loop Logic
                    if (playing && _loopMode.value) {
                        val start = _loopStart.value
                        val end = _loopEnd.value
                        
                        if (start != null && end != null && end > start && currentMs >= end) {
                            controller.seekTo(start)
                            _currentPosition.value = start
                        }
                    }
                }
                // Adaptive polling: fast while A-B looping (tight loop points), slower when idle
                val pollingDelay = when {
                    !playing -> 250L
                    _loopMode.value && _loopStart.value != null && _loopEnd.value != null -> 20L
                    else -> 100L
                }
                kotlinx.coroutines.delay(pollingDelay)
            }
        }
    }


    // Player Controls
    override fun seekTo(position: Long) {
        mediaController?.seekTo(position)
        // Reflect the seek immediately, even while paused.
        _currentPosition.value = position
    }

    override fun skipToNext() {
        mediaController?.seekToNext()
    }

    override fun skipToPrevious() {
        mediaController?.seekToPrevious()
    }

    private val _shuffleModeEnabled = MutableStateFlow(false)
    override val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    override val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    override fun toggleShuffleMode() {
        val controller = mediaController ?: return
        val newMode = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newMode
        _shuffleModeEnabled.value = newMode
    }

    override fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val newMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = newMode
        _repeatMode.value = newMode
    }

    // Equalizer
    private var equalizer: android.media.audiofx.Equalizer? = null
    private val _equalizerBands = MutableStateFlow<List<com.bitvibe.app.domain.player.EqBand>>(emptyList())
    override val equalizerBands: StateFlow<List<com.bitvibe.app.domain.player.EqBand>> = _equalizerBands.asStateFlow()

    private fun initEqualizer(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return
        
        try {
            equalizer?.release()
            equalizer = android.media.audiofx.Equalizer(0, audioSessionId)
            equalizer?.enabled = true
            updateEqualizerBands()
        } catch (e: Exception) {
            Log.e("MusicController", "Error checking equalizer", e)
        }
    }

    private fun updateEqualizerBands() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val minLevel = eq.bandLevelRange[0].toInt()
            val maxLevel = eq.bandLevelRange[1].toInt()
            val bands = mutableListOf<com.bitvibe.app.domain.player.EqBand>()
            
            for (i in 0 until numBands) {
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000
                bands.add(
                    com.bitvibe.app.domain.player.EqBand(
                        id = i,
                        name = "$centerFreq Hz",
                        minLevel = minLevel,
                        maxLevel = maxLevel,
                        currentLevel = eq.getBandLevel(i.toShort()).toInt()
                    )
                )
            }
            _equalizerBands.value = bands
        } catch (e: Exception) {
            Log.e("MusicController", "Error reading bands", e)
        }
    }

    override fun setBandLevel(bandId: Int, level: Int) {
        val eq = equalizer ?: return
        try {
            eq.setBandLevel(bandId.toShort(), level.toShort())
            updateEqualizerBands() // Refresh state
        } catch (e: Exception) {
            Log.e("MusicController", "Error setting band", e)
        }
    }
    // Visualizer
    private var visualizer: android.media.audiofx.Visualizer? = null
    private val _waveform = MutableStateFlow<ByteArray>(ByteArray(0))
    override val waveform: StateFlow<ByteArray> = _waveform.asStateFlow()

    private fun initVisualizer(audioSessionId: Int) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId == 0) return

        try {
            visualizer?.release()
            visualizer = android.media.audiofx.Visualizer(audioSessionId).apply {
                captureSize = android.media.audiofx.Visualizer.getCaptureSizeRange()[1] // Max size
                setDataCaptureListener(
                    object : android.media.audiofx.Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: android.media.audiofx.Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (waveform != null) {
                                _waveform.value = waveform
                            }
                        }

                        override fun onFftDataCapture(
                            visualizer: android.media.audiofx.Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            // Not using FFT for now, just Waveform
                        }
                    },
                    android.media.audiofx.Visualizer.getMaxCaptureRate() / 2,
                    true, // Waveform
                    false // FFT
                )
                enabled = true
            }
        } catch (e: Exception) {
            Log.e("MusicController", "Error initializing visualizer", e)
        }
    }
    
    // Lifecycle cleanup for audio effects
    fun release() {
        try {
            equalizer?.release()
            equalizer = null
            visualizer?.release()
            visualizer = null
            mediaControllerFuture?.let {
                MediaController.releaseFuture(it)
            }
            mediaController = null
            Log.d("MusicController", "Released audio effects and controller")
        } catch (e: Exception) {
            Log.e("MusicController", "Error releasing resources", e)
        }
    }

    private companion object {
        const val EXTRA_DURATION = "DURATION"
        const val MIN_SPEED = 0.25f
        const val MAX_SPEED = 3.0f
    }
}
