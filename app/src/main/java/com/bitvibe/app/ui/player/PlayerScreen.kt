package com.bitvibe.app.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.bitvibe.app.domain.player.MusicController

@Composable
fun PlayerScreen(
    musicController: MusicController,
    onCollapse: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val currentSong by musicController.currentSong.collectAsStateWithLifecycle()
    val isPlaying by musicController.isPlaying.collectAsStateWithLifecycle()
    val waveform by musicController.waveform.collectAsStateWithLifecycle()
    
    var isProMode by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                     Icon(Icons.Filled.Album, contentDescription = "Collapse", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                Text(
                    text = "NOW PLAYING",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(onClick = { /* TODO: Queue */ }) {
                     Icon(Icons.Filled.MusicNote, contentDescription = "Queue", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        bottomBar = {
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pro Mode Toggle - Now a solid Button
                androidx.compose.material3.Button(
                    onClick = { isProMode = !isProMode },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isProMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = if (isProMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (isProMode) "PRO MODE ON" else "PRO MODE",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Album Art (Hidden in Pro Mode)
            AnimatedVisibility(
                visible = !isProMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                     // (Visualizer moved to Pro Mode)

                    
                    // Central Icon Placeholder
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Album, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isProMode) 12.dp else 48.dp))

            // Info
            Text(
                text = currentSong?.title ?: "No Track Playing",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentSong?.artist ?: "BitVibe Player",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Seek Bar with Loop Markers
            val duration = (currentSong?.duration ?: 1L).coerceAtLeast(1L)
            val position by musicController.currentPosition.collectAsStateWithLifecycle()
            val loopStart by musicController.loopStart.collectAsStateWithLifecycle()
            val loopEnd by musicController.loopEnd.collectAsStateWithLifecycle()
            
            Column {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Loop Markers Overlay
                    if (duration > 0 && (loopStart != null || loopEnd != null)) {
                        androidx.compose.foundation.Canvas(
                             modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .align(Alignment.Center)
                        ) {
                            val width = size.width
                            val startX = loopStart?.let { (it.toFloat() / duration) * width }
                            val endX = loopEnd?.let { (it.toFloat() / duration) * width }
                            
                            val markerColor = androidx.compose.ui.graphics.Color(0xFF64B5F6) // Use our primary blue explicitly here or get from theme
                            
                            if (startX != null) {
                                drawCircle(color = markerColor, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(startX, size.height / 2))
                            }
                            if (endX != null) {
                                drawCircle(color = markerColor, radius = 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(endX, size.height / 2))
                            }
                            
                            // Draw connecting line if both exist
                            if (startX != null && endX != null && endX > startX) {
                                drawLine(
                                    color = markerColor.copy(alpha = 0.3f),
                                    start = androidx.compose.ui.geometry.Offset(startX, size.height / 2),
                                    end = androidx.compose.ui.geometry.Offset(endX, size.height / 2),
                                    strokeWidth = 4.dp.toPx()
                                )
                            }
                        }
                    }

                    // Main Seekbar
                    // Note: Slider colors need to be explicit or derived
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Slider(
                        value = (position.toFloat() / duration).coerceIn(0f, 1f),
                        onValueChange = { musicController.seekTo((it * duration).toLong()) },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent, 
                            activeTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f), // White/OnSurface track
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth().height(16.dp)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(position), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatTime(duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Main Controls
            val shuffleEnabled by musicController.shuffleModeEnabled.collectAsStateWithLifecycle()
            val repeatMode by musicController.repeatMode.collectAsStateWithLifecycle()
            val primaryColor = MaterialTheme.colorScheme.primary
            val iconTint = MaterialTheme.colorScheme.onSurfaceVariant

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { musicController.toggleShuffleMode() }) {
                    Icon(Icons.Filled.Shuffle, null, tint = if(shuffleEnabled) primaryColor else iconTint)
                }
                
                IconButton(onClick = { musicController.skipToPrevious() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipPrevious, null, modifier = Modifier.size(32.dp), tint = iconTint)
                }
                
                // Play Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { musicController.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        null,
                        modifier = Modifier.size(36.dp),
                        tint = primaryColor
                    )
                }
                
                IconButton(onClick = { musicController.skipToNext() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.SkipNext, null, modifier = Modifier.size(32.dp), tint = iconTint)
                }
                
                IconButton(onClick = { musicController.toggleRepeatMode() }) {
                    Icon(
                        if(repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, 
                        null, 
                        tint = if(repeatMode != Player.REPEAT_MODE_OFF) primaryColor else iconTint
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            
            // PRO MODE SECTIONS
            AnimatedVisibility(
                visible = isProMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 32.dp)) {
                    // A-B Loop Card
                    ProCard(
                        title = "A-B LOOP", 
                        icon = Icons.Filled.Loop,
                        trailingContent = {
                            val loopStart by musicController.loopStart.collectAsStateWithLifecycle()
                            val loopEnd by musicController.loopEnd.collectAsStateWithLifecycle()
                            if (loopStart != null || loopEnd != null) {
                                TextButton(onClick = { musicController.clearLoop() }) {
                                    Text("CLEAR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    ) {
                         val loopStart by musicController.loopStart.collectAsStateWithLifecycle()
                         val loopEnd by musicController.loopEnd.collectAsStateWithLifecycle()
                         val loopMode by musicController.loopMode.collectAsStateWithLifecycle()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProButton(text = if(loopStart != null) "A: ${formatTime(loopStart!!)}" else "Set A", active = loopStart != null) { musicController.setLoopStart(position) }
                            ProButton(text = if(loopEnd != null) "B: ${formatTime(loopEnd!!)}" else "Set B", active = loopEnd != null) { musicController.setLoopEnd(position) }
                            ProButton(text = "Loop", active = loopMode) { musicController.toggleLoopMode() }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Speed Card
                    val speed by musicController.playbackSpeed.collectAsStateWithLifecycle()
                    ProCard(title = "SPEED", icon = Icons.Filled.Speed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { musicController.setPlaybackSpeed((speed - 0.1f).coerceAtLeast(0.25f)) },
                                modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)
                            ) {
                                Icon(Icons.Filled.Remove, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            Text(
                                "%.2fx".format(speed),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            IconButton(
                                onClick = { musicController.setPlaybackSpeed((speed + 0.1f).coerceAtMost(2.0f)) },
                                modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)
                            ) {
                                Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visualizer Card (Moved here)
                    ProCard(title = "VISUALIZER", icon = Icons.Filled.GraphicEq) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                             if (waveform.isNotEmpty()) {
                                VisualizerView(
                                    waveform = waveform,
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Playing audio...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProCard(
    title: String, 
    icon: ImageVector, 
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurface)
                }
                trailingContent?.invoke()
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ProButton(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
