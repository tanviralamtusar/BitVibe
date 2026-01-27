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
                updateCurrentSong(mediaItem)
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
    }

    private fun updateCurrentSong(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            _currentSong.value = null
            return
        }
        
        // Map back to AudioFile (Basic mapping for now)
        // Ideally we pass full metadata in MediaItem
        val metadata = mediaItem.mediaMetadata
        val id = mediaItem.mediaId.toLongOrNull() ?: 0L
        
        // We might not have all info here if we don't put it in MediaItem properly.
        // For MVP, assume we can reconstruct or simple display.
        val duration = metadata.extras?.getLong("DURATION") ?: -1L
        
        // Fallback to controller duration if current item matches
        val finalDuration = if (duration > 0) duration else {
             if (mediaController?.currentMediaItem?.mediaId == mediaItem.mediaId) {
                 mediaController?.duration ?: 0L 
             } else 0L
        }

        _currentSong.value = AudioFile(
            id = id,
            title = metadata.title?.toString() ?: "Unknown",
            artist = metadata.artist?.toString() ?: "Unknown",
            album = metadata.albumTitle?.toString() ?: "",
            duration = if (finalDuration > 0) finalDuration else 0L, 
            path = "", 
            contentUri = Uri.parse(mediaItem.mediaId) 
        )
    }

    override fun play(audioFile: AudioFile) {
        val controller = mediaController ?: return
        
        val mediaItem = MediaItem.Builder()
            .setMediaId(audioFile.id.toString())
            .setUri(audioFile.contentUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(audioFile.title)
                    .setArtist(audioFile.artist)
                    .setAlbumTitle(audioFile.album)
                    .setArtworkUri(audioFile.albumArtUri)
                    .setExtras(android.os.Bundle().apply {
                        putLong("DURATION", audioFile.duration)
                    })
                    .build()
            )
            .build()

        controller.setMediaItem(mediaItem)
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
            controller.play()
        }
    }

    // Speed
    private val _playbackSpeed = MutableStateFlow(1f)
    override val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    override fun setPlaybackSpeed(speed: Float) {
        mediaController?.playbackParameters = androidx.media3.common.PlaybackParameters(speed, 1f)
        _playbackSpeed.value = speed
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
    }

    override fun setLoopEnd(ms: Long) {
        _loopEnd.value = ms
    }

    override fun clearLoop() {
        _loopStart.value = null
        _loopEnd.value = null
        _loopMode.value = false
    }

    override fun toggleLoopMode() {
        _loopMode.value = !_loopMode.value
    }
    
    // Polling for Position & Loop Check (Adaptive polling for efficiency)
    init {
        scope.launch {
            while (true) {
                if (mediaController?.isPlaying == true) {
                    val currentMs = mediaController?.currentPosition ?: 0L
                    _currentPosition.value = currentMs
                    
                    // Loop Logic
                    if (_loopMode.value) {
                        val start = _loopStart.value
                        val end = _loopEnd.value
                        
                        if (start != null && end != null && end > start) {
                            if (currentMs >= end) {
                                mediaController?.seekTo(start)
                            }
                        }
                    }
                }
                // Adaptive polling: faster when looping (for precise loop points), slower otherwise
                val pollingDelay = if (_loopMode.value && _loopStart.value != null && _loopEnd.value != null) {
                    50L // 20Hz for precise A-B loop
                } else {
                    100L // 10Hz for normal playback (saves CPU)
                }
                kotlinx.coroutines.delay(pollingDelay)
            }
        }
    }


    // Player Controls
    override fun seekTo(position: Long) {
        mediaController?.seekTo(position)
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
        // State update will come from listener, but optimistic update is fine too
    }

    override fun toggleRepeatMode() {
        val controller = mediaController ?: return
        val newMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = newMode
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
}
