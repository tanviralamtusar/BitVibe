package com.bitvibe.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Library : Screen("library", "Library", Icons.Filled.LibraryMusic)
    object Folders : Screen("folders", "Folders", Icons.Filled.Folder)
    object Playlists : Screen("playlists", "Playlists", Icons.Filled.List)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}
