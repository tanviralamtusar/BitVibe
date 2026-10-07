package com.bitvibe.app.ui.screens.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bitvibe.app.data.youtube.YouTubeVideo
import com.bitvibe.app.ui.player.PillButton
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey
import com.bitvibe.app.ui.theme.TextMuted
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

private enum class YouTubeTab(val label: String) { Results("Results"), Channel("More from channel"), Favorites("Favorites") }

/**
 * YouTube search and playback through YouTube's official embedded player. Per YouTube's terms the
 * player stays visible, keeps YouTube's ads and controls, and stops when you leave the app or the
 * tab. Nothing is downloaded and the audio isn't routed through BeatVibe's own player.
 */
@Composable
fun YouTubeScreen(viewModel: YouTubeViewModel = hiltViewModel()) {
    val search by viewModel.search.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val channelVideos by viewModel.channelVideos.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(YouTubeTab.Results) }
    val focusManager = LocalFocusManager.current

    // Show the channel's other videos as soon as one is picked; fall back when the player closes.
    LaunchedEffect(selected?.id) {
        if (selected == null && tab == YouTubeTab.Channel) tab = YouTubeTab.Results
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ──────────────────────────────────
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.OndemandVideo, contentDescription = null, tint = BitVibeCyan, modifier = Modifier.size(30.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("YouTube", style = MaterialTheme.typography.headlineLarge, color = BitVibeCyan)
        }

        // ── Player (official embed, always visible while playing) ──
        selected?.let { video ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                YouTubeEmbed(
                    videoId = video.id,
                    onPlaying = viewModel::onYouTubePlaybackStarted,
                    modifier = Modifier.fillMaxSize()
                )
            }
            val isFavorite = favorites.any { it.id == video.id }
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(video.channelTitle, style = MaterialTheme.typography.bodySmall, color = TextGrey, maxLines = 1)
                }
                IconButton(onClick = { viewModel.toggleFavorite(video, isFavorite) }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) BitVibeCyan else TextGrey
                    )
                }
                IconButton(onClick = viewModel::closePlayer) {
                    Icon(Icons.Filled.Close, contentDescription = "Close video", tint = TextGrey)
                }
            }
        }

        // ── Search ──────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs on YouTube", color = TextMuted) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = TextGrey) },
            singleLine = true,
            enabled = viewModel.isConfigured,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                viewModel.search(query)
                tab = YouTubeTab.Results
                focusManager.clearFocus()
            }),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = BitVibeCyan,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = BitVibeCyan
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        // ── Tabs ────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            YouTubeTab.entries
                .filter { it != YouTubeTab.Channel || selected != null }
                .forEach { t ->
                    PillButton(
                        text = t.label,
                        active = t == tab,
                        accent = BitVibeCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { tab = t }
                    )
                }
        }

        // ── List ────────────────────────────────────
        val videos = when (tab) {
            YouTubeTab.Results -> search.videos
            YouTubeTab.Channel -> channelVideos
            YouTubeTab.Favorites -> favorites
        }
        LazyColumn(
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (!viewModel.isConfigured) {
                item { NotConfiguredNotice() }
            } else if (videos.isEmpty()) {
                item {
                    EmptyText(
                        when {
                            tab == YouTubeTab.Results && search.loading -> null
                            tab == YouTubeTab.Results && search.error != null -> search.error
                            tab == YouTubeTab.Results && !search.searched -> "Search for a song, artist or album."
                            tab == YouTubeTab.Results -> "No results for \"${search.query}\"."
                            tab == YouTubeTab.Channel -> "Loading more from this channel…"
                            else -> "Tap ♡ on a video to save it here."
                        }
                    )
                }
            }
            items(videos, key = { "${tab.name}:${it.id}" }) { video ->
                VideoRow(
                    video = video,
                    playing = video.id == selected?.id,
                    onClick = { viewModel.select(video) }
                )
            }
            if (tab == YouTubeTab.Results && search.videos.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        when {
                            search.loading -> CircularProgressIndicator(color = BitVibeCyan, modifier = Modifier.size(28.dp))
                            search.error != null -> Text(search.error!!, color = TextGrey, style = MaterialTheme.typography.bodySmall)
                            search.nextPageToken != null -> TextButton(onClick = viewModel::loadMore) {
                                Text("Load more", color = BitVibeCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** YouTube's IFrame player. Pauses when the app goes to the background and is released with the screen. */
@Composable
private fun YouTubeEmbed(videoId: String, onPlaying: () -> Unit, modifier: Modifier = Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    val currentOnPlaying by rememberUpdatedState(onPlaying)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            YouTubePlayerView(context).apply {
                enableAutomaticInitialization = false
                // Ties the player to the activity: it pauses on stop (no background play).
                lifecycleOwner.lifecycle.addObserver(this)
                initialize(
                    object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            player = youTubePlayer
                        }

                        override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                            if (state == PlayerConstants.PlayerState.PLAYING) currentOnPlaying()
                        }
                    },
                    IFramePlayerOptions.Builder().controls(1).fullscreen(0).build()
                )
            }
        },
        onRelease = { view ->
            lifecycleOwner.lifecycle.removeObserver(view)
            view.release()
        }
    )

    // Load (and autoplay) whenever the selected video changes, once the player is ready.
    LaunchedEffect(videoId, player) {
        player?.loadVideo(videoId, 0f)
    }
}

@Composable
private fun VideoRow(video: YouTubeVideo, playing: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (playing) BitVibeCyan.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(128.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (playing) FontWeight.Bold else FontWeight.Medium,
                color = if (playing) BitVibeCyan else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(video.channelTitle, style = MaterialTheme.typography.bodySmall, color = TextGrey, maxLines = 1)
        }
    }
}

@Composable
private fun EmptyText(text: String?) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        if (text == null) {
            CircularProgressIndicator(color = BitVibeCyan, modifier = Modifier.size(28.dp))
        } else {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = TextGrey)
        }
    }
}

@Composable
private fun NotConfiguredNotice() {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "YouTube search isn't set up in this build",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "It needs a free YouTube Data API key. See \"YouTube\" in the README for the two-minute setup.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGrey
        )
    }
}
