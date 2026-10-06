package com.bitvibe.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.outlined.CloseFullscreen
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.bitvibe.app.domain.player.MusicController
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted

@Composable
fun PlayerScreen(
    musicController: MusicController,
    onCollapse: () -> Unit
) {
    val currentSong by musicController.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicController.isPlaying.collectAsStateWithLifecycle()
    val waveform by musicController.waveform.collectAsStateWithLifecycle()

    var isProMode by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top Bar ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    letterSpacing = 1.sp
                )
                Text(
                    text = currentSong?.album?.takeIf { it.isNotBlank() } ?: "BitVibe",
                    style = MaterialTheme.typography.titleSmall,
                    color = BitVibeCyan,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // ── Album Art ───────────────────────────────────
        AnimatedVisibility(
            visible = !isProMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 28.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder sits underneath, so songs without embedded art still show something.
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(80.dp)
                )
                if (currentSong?.albumArtUri != null) {
                    AsyncImage(
                        model = currentSong?.albumArtUri,
                        contentDescription = currentSong?.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(if (isProMode) 8.dp else 20.dp))

        // ── Song Info ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "No Track Playing",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentSong?.artist ?: "BitVibe Player",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = { currentSong?.let { shareSong(context, it) } },
                    enabled = currentSong != null
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share", tint = TextGrey, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Seekbar (Cyan) ──────────────────────────────
        val duration = (currentSong?.duration ?: 1L).coerceAtLeast(1L)
        val position by musicController.currentPosition.collectAsStateWithLifecycle()
        val loopStart by musicController.loopStart.collectAsStateWithLifecycle()
        val loopEnd by musicController.loopEnd.collectAsStateWithLifecycle()

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            // Loop markers (above seekbar)
            if (duration > 0 && (loopStart != null || loopEnd != null)) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .padding(horizontal = 10.dp)
                ) {
                    val w = size.width
                    val startX = loopStart?.let { (it.toFloat() / duration) * w }
                    val endX = loopEnd?.let { (it.toFloat() / duration) * w }
                    val path = androidx.compose.ui.graphics.Path()
                    val triSize = 5.dp.toPx()
                    if (startX != null) {
                        path.reset()
                        path.moveTo(startX, size.height)
                        path.lineTo(startX - triSize, 0f)
                        path.lineTo(startX + triSize, 0f)
                        path.close()
                        drawPath(path, color = BitVibeCyan)
                    }
                    if (endX != null) {
                        path.reset()
                        path.moveTo(endX, size.height)
                        path.lineTo(endX - triSize, 0f)
                        path.lineTo(endX + triSize, 0f)
                        path.close()
                        drawPath(path, color = BitVibeCyan)
                    }
                }
            }

            // Seekbar: track the finger locally and seek once on release, so dragging
            // doesn't fire dozens of seeks (audible stutter) or jump back while held.
            var dragFraction by remember { mutableFloatStateOf(-1f) }
            val isDragging = dragFraction >= 0f
            val shownPosition = if (isDragging) (dragFraction * duration).toLong() else position
            Slider(
                value = if (isDragging) dragFraction else (position.toFloat() / duration).coerceIn(0f, 1f),
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    if (dragFraction >= 0f) musicController.seekTo((dragFraction * duration).toLong())
                    dragFraction = -1f
                },
                enabled = currentSong != null,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = BitVibeCyan,
                    inactiveTrackColor = BitVibeCyan.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatTime(shownPosition), style = MaterialTheme.typography.bodySmall, color = TextGrey)
                Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = TextGrey)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Main Controls ───────────────────────────────
        val shuffleEnabled by musicController.shuffleModeEnabled.collectAsStateWithLifecycle()
        val repeatMode by musicController.repeatMode.collectAsStateWithLifecycle()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { musicController.toggleRepeatMode() }) {
                Icon(
                    if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    },
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) BitVibeCyan else TextGrey,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { musicController.toggleShuffleMode() }) {
                Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) BitVibeCyan else TextGrey, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = { musicController.skipToPrevious() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(36.dp), tint = Color.White)
            }

            // Big cyan play button
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(BitVibeCyan)
                    .clickable { musicController.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }

            IconButton(onClick = { musicController.skipToNext() }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(36.dp), tint = Color.White)
            }
            IconButton(onClick = { isProMode = !isProMode }) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = "Pro tools",
                    tint = if (isProMode) BitVibeCyan else TextGrey,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = { showAddToPlaylist = true }, enabled = currentSong != null) {
                Icon(Icons.Filled.Add, contentDescription = "Add to playlist", tint = TextGrey, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Pro Toggle ──────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Pro",
                style = MaterialTheme.typography.titleMedium,
                color = if (isProMode) BitVibeCyan else TextGrey,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { isProMode = !isProMode }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // ── Pro Mode Panel ──────────────────────────────
        AnimatedVisibility(
            visible = isProMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                // ── A-B Loop ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Loop, null, tint = BitVibeCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("A-B Loop", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }

                val loopMode by musicController.loopMode.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyanPillButton(
                        text = if (loopStart != null) "A: ${formatTime(loopStart!!)}" else "Set A",
                        active = loopStart != null,
                        modifier = Modifier.weight(1f)
                    ) { musicController.setLoopStart(position) }
                    CyanPillButton(
                        text = if (loopEnd != null) "B: ${formatTime(loopEnd!!)}" else "Set B",
                        active = loopEnd != null,
                        modifier = Modifier.weight(1f)
                    ) { musicController.setLoopEnd(position) }
                    CyanPillButton(
                        text = "Loop",
                        active = loopMode,
                        modifier = Modifier.weight(1f)
                    ) { musicController.toggleLoopMode() }
                    CyanPillButton(
                        text = "Reset",
                        active = false,
                        modifier = Modifier.weight(1f)
                    ) { musicController.clearLoop() }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Speed + Equalizer (side by side) ────
                val equalizerBands by musicController.equalizerBands.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Speed card (left)
                    val speed by musicController.playbackSpeed.collectAsStateWithLifecycle()
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BitVibeCyan.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Speed, null, tint = BitVibeCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Speed", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, BitVibeCyan.copy(alpha = 0.5f), CircleShape)
                                        .clickable { musicController.setPlaybackSpeed(speed - 0.1f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Remove, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                // Tap the value to snap back to normal speed.
                                Text(
                                    "%.2fx".format(speed),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (speed == 1f) Color.White else BitVibeCyan,
                                    modifier = Modifier.clickable { musicController.setPlaybackSpeed(1f) }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, BitVibeCyan.copy(alpha = 0.5f), CircleShape)
                                        .clickable { musicController.setPlaybackSpeed(speed + 0.1f) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    // Equalizer toggle (right)
                    Card(
                        onClick = { showEqualizer = !showEqualizer },
                        enabled = equalizerBands.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BitVibeCyan.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Equalizer",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BitVibeCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    if (showEqualizer) Icons.Outlined.CloseFullscreen else Icons.Outlined.OpenInFull,
                                    contentDescription = if (showEqualizer) "Hide equalizer" else "Show equalizer",
                                    tint = TextGrey,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (equalizerBands.isEmpty()) "Available once playback starts"
                                else "${equalizerBands.size} bands · tap to ${if (showEqualizer) "hide" else "adjust"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGrey
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Full Equalizer (expandable) ─────────
                if (showEqualizer && equalizerBands.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BitVibeCyan.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.GraphicEq, null, tint = BitVibeCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("EQUALIZER", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                equalizerBands.forEach { band ->
                                    EqBandSlider(band = band) { level ->
                                        musicController.setBandLevel(band.id, level)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                EqPresetButton("Flat") { equalizerBands.forEach { musicController.setBandLevel(it.id, 0) } }
                                EqPresetButton("Bass+") {
                                    equalizerBands.forEachIndexed { i, b ->
                                        musicController.setBandLevel(b.id, when (i) { 0 -> (b.maxLevel * 0.7).toInt(); 1 -> (b.maxLevel * 0.5).toInt(); else -> 0 })
                                    }
                                }
                                EqPresetButton("Vocal") {
                                    equalizerBands.forEachIndexed { i, b ->
                                        val mid = equalizerBands.size / 2
                                        musicController.setBandLevel(b.id, if (i in (mid - 1)..(mid + 1)) (b.maxLevel * 0.4).toInt() else (b.minLevel * 0.2).toInt())
                                    }
                                }
                                EqPresetButton("Rock") {
                                    equalizerBands.forEachIndexed { i, b ->
                                        musicController.setBandLevel(b.id, when (i) { 0 -> (b.maxLevel * 0.5).toInt(); 1 -> (b.maxLevel * 0.3).toInt(); equalizerBands.size - 1 -> (b.maxLevel * 0.6).toInt(); equalizerBands.size - 2 -> (b.maxLevel * 0.4).toInt(); else -> 0 })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    val song = currentSong
    if (showAddToPlaylist && song != null) {
        AddToPlaylistDialog(song = song, onDismiss = { showAddToPlaylist = false })
    }
}

private fun shareSong(context: android.content.Context, song: com.bitvibe.app.data.model.AudioFile) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "audio/*"
        putExtra(android.content.Intent.EXTRA_STREAM, song.contentUri)
        putExtra(android.content.Intent.EXTRA_TEXT, "${song.title} — ${song.artist}")
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(android.content.Intent.createChooser(intent, "Share song"))
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No app available to share", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// ── Cyan outlined pill button ───────────────────────────
@Composable
fun CyanPillButton(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 1.dp,
                color = BitVibeCyan,
                shape = RoundedCornerShape(6.dp)
            )
            .background(if (active) BitVibeCyan.copy(alpha = 0.15f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = BitVibeCyan,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun EqBandSlider(
    band: com.bitvibe.app.domain.player.EqBand,
    onValueChange: (Int) -> Unit
) {
    val range = band.maxLevel - band.minLevel
    val normalizedLevel = if (range > 0) (band.currentLevel - band.minLevel).toFloat() / range else 0.5f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        // Custom vertical slider drawn with Canvas
        Box(
            modifier = Modifier
                .height(110.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BitVibeCyan.copy(alpha = 0.08f))
                .clickable { /* handle via drag below */ },
            contentAlignment = Alignment.BottomCenter
        ) {
            // Track background
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val trackWidth = 3.dp.toPx()
                val centerX = size.width / 2
                // Inactive track
                drawLine(
                    color = BitVibeCyan.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                    end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                    strokeWidth = trackWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // Active track (from bottom)
                val activeHeight = size.height * normalizedLevel
                drawLine(
                    color = BitVibeCyan,
                    start = androidx.compose.ui.geometry.Offset(centerX, size.height),
                    end = androidx.compose.ui.geometry.Offset(centerX, size.height - activeHeight),
                    strokeWidth = trackWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                // Thumb dot
                val thumbY = size.height - activeHeight
                drawCircle(
                    color = BitVibeCyan,
                    radius = 5.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(centerX, thumbY)
                )
            }

            // Invisible slider for drag interaction
            Slider(
                value = normalizedLevel,
                onValueChange = { fraction ->
                    val level = (band.minLevel + (fraction * range)).toInt()
                    onValueChange(level)
                },
                modifier = Modifier
                    .graphicsLayer {
                        rotationZ = 270f
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    }
                    .width(110.dp)
                    .graphicsLayer { alpha = 0f },  // invisible, handles touch only
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = band.name.replace(" Hz", "").let {
                val freq = it.toIntOrNull() ?: 0
                if (freq >= 1000) "${freq / 1000}k" else it
            },
            style = MaterialTheme.typography.labelSmall,
            color = TextGrey,
            maxLines = 1
        )
    }
}

@Composable
fun EqPresetButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, BitVibeCyan, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = BitVibeCyan)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
