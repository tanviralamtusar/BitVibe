package com.bitvibe.app.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitvibe.app.data.model.AudioFile
import com.bitvibe.app.domain.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val musicController: com.bitvibe.app.domain.player.MusicController
) : ViewModel() {

    val audioFiles: StateFlow<List<AudioFile>> = repository.getAllAudio()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Each screen gets its own ViewModel; only the first one needs to hit MediaStore.
        viewModelScope.launch {
            repository.scanMediaIfNeeded()
        }
    }

    fun scanMedia() {
        viewModelScope.launch {
            repository.scanMedia()
        }
    }
    
    /** Plays [audioFile] with [queue] loaded, so next/previous move through the list it came from. */
    fun playSong(audioFile: AudioFile, queue: List<AudioFile> = listOf(audioFile)) {
        musicController.play(audioFile, queue)
    }
}
