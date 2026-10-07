package com.bitvibe.app.ui.screens.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitvibe.app.data.youtube.YouTubeApi
import com.bitvibe.app.data.youtube.YouTubeApiException
import com.bitvibe.app.data.youtube.YouTubeFavoriteDao
import com.bitvibe.app.data.youtube.YouTubeVideo
import com.bitvibe.app.data.youtube.toFavorite
import com.bitvibe.app.domain.player.MusicController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class YouTubeSearchState(
    val query: String = "",
    val videos: List<YouTubeVideo> = emptyList(),
    val nextPageToken: String? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val searched: Boolean = false
)

@HiltViewModel
class YouTubeViewModel @Inject constructor(
    private val api: YouTubeApi,
    private val favoriteDao: YouTubeFavoriteDao,
    private val musicController: MusicController
) : ViewModel() {

    val isConfigured: Boolean get() = api.isConfigured

    private val _search = MutableStateFlow(YouTubeSearchState())
    val search: StateFlow<YouTubeSearchState> = _search.asStateFlow()

    private val _selected = MutableStateFlow<YouTubeVideo?>(null)
    val selected: StateFlow<YouTubeVideo?> = _selected.asStateFlow()

    /** "More from this channel" for the selected video (YouTube's API has no "related videos"). */
    private val _channelVideos = MutableStateFlow<List<YouTubeVideo>>(emptyList())
    val channelVideos: StateFlow<List<YouTubeVideo>> = _channelVideos.asStateFlow()

    val favorites: StateFlow<List<YouTubeVideo>> = favoriteDao.getAll()
        .map { list -> list.map { it.toVideo() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null
    private var channelJob: Job? = null
    // Searches cost API quota; repeat queries in a session are served from memory.
    private val cache = mutableMapOf<String, com.bitvibe.app.data.youtube.YouTubeSearchPage>()

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        searchJob?.cancel()
        _search.value = YouTubeSearchState(query = q, loading = true, searched = true)
        searchJob = viewModelScope.launch {
            _search.value = try {
                val page = cache[q] ?: api.search(q).also { cache[q] = it }
                _search.value.copy(videos = page.videos, nextPageToken = page.nextPageToken, loading = false)
            } catch (e: YouTubeApiException) {
                _search.value.copy(loading = false, error = e.message)
            } catch (e: Exception) {
                _search.value.copy(loading = false, error = "Couldn't reach YouTube. Check your connection.")
            }
        }
    }

    fun loadMore() {
        val state = _search.value
        val token = state.nextPageToken ?: return
        if (state.loading) return
        _search.value = state.copy(loading = true)
        searchJob = viewModelScope.launch {
            _search.value = try {
                val page = api.search(state.query, pageToken = token)
                _search.value.copy(
                    videos = (state.videos + page.videos).distinctBy { it.id },
                    nextPageToken = page.nextPageToken,
                    loading = false
                )
            } catch (e: Exception) {
                _search.value.copy(loading = false, error = (e as? YouTubeApiException)?.message ?: "Couldn't load more results.")
            }
        }
    }

    fun select(video: YouTubeVideo) {
        if (_selected.value?.id == video.id) return
        _selected.value = video
        _channelVideos.value = emptyList()
        channelJob?.cancel()
        if (video.channelId.isBlank()) return
        channelJob = viewModelScope.launch {
            _channelVideos.value = try {
                api.search(query = "", channelId = video.channelId).videos.filter { it.id != video.id }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    fun closePlayer() {
        _selected.value = null
        _channelVideos.value = emptyList()
    }

    /** Only one thing plays at a time: a YouTube video starting pauses BeatVibe's own music. */
    fun onYouTubePlaybackStarted() {
        if (musicController.isPlaying.value) musicController.pause()
    }

    fun toggleFavorite(video: YouTubeVideo, isFavorite: Boolean) {
        viewModelScope.launch {
            if (isFavorite) favoriteDao.delete(video.id) else favoriteDao.insert(video.toFavorite())
        }
    }
}
