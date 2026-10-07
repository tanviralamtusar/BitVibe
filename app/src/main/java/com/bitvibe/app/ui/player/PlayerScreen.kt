package com.bitvibe.app.ui.player

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.domain.player.MusicController
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Full-screen player, laid out like Poweramp: a big swipeable cover tinted into the background,
 * track info, a wave seek bar, transport controls and a compact action row. BeatVibe's practice
 * tools (A-B loop, speed, equalizer) open in a bottom sheet instead of crowding the screen.
 */
@Composable
fun PlayerScreen(
    musicController: MusicController,
    onCollapse: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val song by musicController.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicController.isPlaying.collectAsStateWithLifecycle()
    val position by musicController.currentPosition.collectAsStateWithLifecycle()
    val loopStart by musicController.loopStart.collectAsStateWithLifecycle()
    val loopEnd by musicController.loopEnd.collectAsStateWithLifecycle()
    val loopMode by musicController.loopMode.collectAsStateWithLifecycle()
    val speed by musicController.playbackSpeed.collectAsStateWithLifecycle()
    val shuffleEnabled by musicController.shuffleModeEnabled.collectAsStateWithLifecycle()
    val repeatMode by musicController.repeatMode.collectAsStateWithLifecycle()
    val queueIndex by musicController.queueIndex.collectAsStateWithLifecycle()
    val queueSize by musicController.queueSize.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var openTool by remember { mutableStateOf<PlayerTool?>(null) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

    // Per-song visuals: accent colour from the cover and the wave seek levels.
    val currentId = song?.id
    val artAccent by produceState<Int?>(initialValue = null, currentId) {
        value = song?.let { viewModel.accentColor(it) }
    }
    val accent by animateColorAsState(
        targetValue = artAccent?.let { Color(it) } ?: BitVibeCyan,
        animationSpec = tween(600),
        label = "accent"
    )
    val levels by produceState<FloatArray?>(initialValue = null, currentId) {
        value = null
        value = song?.let { viewModel.waveform(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Backdrop: blurred cover fading into the background. Blur needs Android 12+;
        // older versions get just the accent gradient rather than a sharp, busy copy of the cover.
        song?.albumArtUri?.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }?.let { art ->
            AsyncImage(
                model = art,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .blur(60.dp)
                    .graphicsLayer { alpha = 0.45f }
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.22f),
                        0.5f to MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top bar ─────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse player",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGrey,
                        letterSpacing = 1.5.sp
                    )
                    if (queueSize > 1) {
                        Text(
                            "${queueIndex + 1} / $queueSize",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = { song?.let { shareSong(context, it) } }, enabled = song != null) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = TextGrey)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Cover: swipe for previous/next, double-tap to play/pause ──
            CoverArt(
                song = song,
                accent = accent,
                onNext = { musicController.skipToNext() },
                onPrevious = { musicController.skipToPrevious() },
                onTogglePlay = { musicController.togglePlayPause() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Track info ──────────────────────────────
            TrackInfo(song = song, speed = speed, loopMode = loopMode, accent = accent)

            Spacer(modifier = Modifier.height(10.dp))

            // ── Wave seek bar ───────────────────────────
            WaveSeekBar(
                levels = levels,
                positionMs = position,
                durationMs = song?.duration ?: 0L,
                loopStartMs = loopStart,
                loopEndMs = loopEnd,
                loopActive = loopMode,
                accent = accent,
                enabled = song != null,
                onSeek = { musicController.seekTo(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Transport ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { musicController.toggleShuffleMode() }) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = if (shuffleEnabled) "Shuffle on" else "Shuffle off",
                        tint = if (shuffleEnabled) accent else TextGrey
                    )
                }
                IconButton(onClick = { musicController.skipToPrevious() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(12.dp, CircleShape, ambientColor = accent, spotColor = accent)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable { musicController.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { musicController.skipToNext() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { musicController.toggleRepeatMode() }) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> "Repeat one"
                            Player.REPEAT_MODE_ALL -> "Repeat all"
                            else -> "Repeat off"
                        },
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) accent else TextGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Action row ──────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(Icons.Filled.GraphicEq, "EQ", active = false, accent = accent) {
                    openTool = PlayerTool.Equalizer
                }
                ActionButton(
                    Icons.Filled.Loop,
                    "A-B",
                    active = loopMode || loopStart != null,
                    accent = accent
                ) { openTool = PlayerTool.Loop }
                ActionButton(
                    Icons.Filled.Speed,
                    if (speed == 1f) "Speed" else "%.2fx".format(speed),
                    active = speed != 1f,
                    accent = accent
                ) { openTool = PlayerTool.Speed }
                ActionButton(
                    Icons.AutoMirrored.Filled.PlaylistAdd,
                    "Add",
                    active = false,
                    accent = accent,
                    enabled = song != null
                ) { showAddToPlaylist = true }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    openTool?.let { tool ->
        PlayerToolsSheet(
            musicController = musicController,
            tool = tool,
            accent = accent,
            onToolChange = { openTool = it },
            onDismiss = { openTool = null }
        )
    }

    val current = song
    if (showAddToPlaylist && current != null) {
        AddToPlaylistDialog(song = current, onDismiss = { showAddToPlaylist = false })
    }
}

@Composable
private fun CoverArt(
    song: AudioFile?,
    accent: Color,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                // Square, as large as the free space allows (height-limited on short screens).
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = offsetX.value / 80f
                    alpha = 1f - (abs(offsetX.value) / size.width.coerceAtLeast(1f)).coerceIn(0f, 0.6f)
                }
                .shadow(24.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .pointerInput(song?.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val width = size.width.toFloat()
                                val threshold = width * 0.25f
                                when {
                                    offsetX.value <= -threshold -> {
                                        offsetX.animateTo(-width, tween(150))
                                        onNext()
                                        offsetX.snapTo(width)
                                    }
                                    offsetX.value >= threshold -> {
                                        offsetX.animateTo(width, tween(150))
                                        onPrevious()
                                        offsetX.snapTo(-width)
                                    }
                                }
                                offsetX.animateTo(0f, spring())
                            }
                        },
                        onDragCancel = { scope.launch { offsetX.animateTo(0f, spring()) } }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { onTogglePlay() })
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = accent.copy(alpha = 0.45f),
                modifier = Modifier.size(96.dp)
            )
            Crossfade(targetState = song?.albumArtUri, label = "cover") { art ->
                if (art != null) {
                    AsyncImage(
                        model = art,
                        contentDescription = song?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackInfo(song: AudioFile?, speed: Float, loopMode: Boolean, accent: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = song?.title ?: "Nothing playing",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            // Long titles scroll instead of being cut off.
            modifier = Modifier.basicMarquee()
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = song?.let { listOf(it.artist, it.album).filter { s -> s.isNotBlank() }.joinToString(" · ") }
                ?: "Pick a song from your library",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGrey,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Format / status line, like Poweramp's info row.
        val tags = buildList {
            song?.path?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length <= 5 }?.let { add(it.uppercase()) }
            if (speed != 1f) add("%.2fx".format(speed))
            if (loopMode) add("A-B LOOP")
        }
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { tag ->
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = when {
        !enabled -> TextMuted
        active -> accent
        else -> TextGrey
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .width(56.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

private fun shareSong(context: Context, song: AudioFile) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(Intent.EXTRA_STREAM, song.contentUri)
        putExtra(Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share song"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show()
    }
}
