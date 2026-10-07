package com.bitvibe.app.data.waveform

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Loudness bars for the player's wave seek bar.
 *
 * Decoding a whole track would take seconds for long files, so we sample: seek to [BARS] evenly
 * spaced points and measure the RMS level of a short decoded chunk at each. That's enough for a
 * recognisable shape (quiet intros, drops, silences) in well under a second for typical files.
 */
@Singleton
class WaveformRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = LruCache<Long, FloatArray>(48)

    /** Levels in 0..1, one per bar. Falls back to a stand-in shape if the file can't be decoded. */
    suspend fun waveform(audioId: Long, uri: Uri, durationMs: Long): FloatArray {
        cache.get(audioId)?.let { return it }
        val levels = withContext(Dispatchers.IO) {
            try {
                extract(uri, durationMs)
            } catch (e: Exception) {
                Log.w(TAG, "Waveform extraction failed for $uri", e)
                null
            }
        } ?: placeholder(audioId)
        cache.put(audioId, levels)
        return levels
    }

    private suspend fun extract(uri: Uri, durationMs: Long): FloatArray? {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
                format.getLong(MediaFormat.KEY_DURATION)
            } else {
                durationMs * 1000
            }
            if (durationUs <= 0) return null

            codec = MediaCodec.createDecoderByType(mime).apply {
                configure(format, null, null, 0)
                start()
            }

            val levels = FloatArray(BARS)
            for (i in 0 until BARS) {
                coroutineContext.ensureActive()
                val timeUs = (durationUs * (i + 0.5) / BARS).toLong()
                extractor.seekTo(timeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
                codec.flush()
                levels[i] = measureChunk(codec, extractor)
            }
            return normalize(levels)
        } finally {
            try {
                codec?.stop()
            } catch (_: Exception) {
            }
            codec?.release()
            extractor.release()
        }
    }

    /** Decodes from the current extractor position until ~[SAMPLES_PER_BAR] samples; returns their RMS. */
    private fun measureChunk(codec: MediaCodec, extractor: MediaExtractor): Float {
        val info = MediaCodec.BufferInfo()
        var sumSquares = 0.0
        var count = 0
        var inputDone = false
        repeat(MAX_DECODE_STEPS) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(TIMEOUT_US)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex) ?: return@repeat
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        codec.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
            if (outIndex >= 0) {
                val out = codec.getOutputBuffer(outIndex)
                if (out != null && info.size > 0) {
                    out.position(info.offset)
                    out.limit(info.offset + info.size)
                    val isFloat = codec.outputFormat.let {
                        it.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            it.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    }
                    if (isFloat) {
                        val floats = out.order(ByteOrder.nativeOrder()).asFloatBuffer()
                        while (floats.hasRemaining() && count < SAMPLES_PER_BAR) {
                            val v = floats.get().toDouble()
                            sumSquares += v * v
                            count++
                        }
                    } else {
                        val shorts = out.order(ByteOrder.nativeOrder()).asShortBuffer()
                        while (shorts.hasRemaining() && count < SAMPLES_PER_BAR) {
                            val v = shorts.get() / 32768.0
                            sumSquares += v * v
                            count++
                        }
                    }
                }
                val ended = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                codec.releaseOutputBuffer(outIndex, false)
                if (count >= SAMPLES_PER_BAR || ended) {
                    return if (count == 0) 0f else sqrt(sumSquares / count).toFloat()
                }
            }
        }
        return if (count == 0) 0f else sqrt(sumSquares / count).toFloat()
    }

    /** Scales so the loudest bar is 1, with a gentle curve so quiet parts stay visible. */
    private fun normalize(levels: FloatArray): FloatArray? {
        val peak = levels.maxOrNull() ?: return null
        if (peak <= 0f) return null
        return FloatArray(levels.size) { i -> max(MIN_LEVEL, sqrt(levels[i] / peak)) }
    }

    /** A stable, music-like stand-in when a format can't be decoded. */
    private fun placeholder(seed: Long): FloatArray {
        val random = java.util.Random(seed)
        var value = 0.5f
        return FloatArray(BARS) {
            value = (value + (random.nextFloat() - 0.5f) * 0.35f).coerceIn(0.25f, 1f)
            value
        }
    }

    private companion object {
        const val TAG = "WaveformRepository"
        const val BARS = 96
        const val SAMPLES_PER_BAR = 4096
        const val MAX_DECODE_STEPS = 40
        const val TIMEOUT_US = 5_000L
        const val MIN_LEVEL = 0.06f
    }
}
