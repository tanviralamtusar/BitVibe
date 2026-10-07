package com.bitvibe.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    // ── Bottom Nav Tabs ───────────────────────────────
    object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    object Explore : Screen("explore", "Explore", Icons.Outlined.Search, Icons.Filled.Search)
    object YouTube : Screen("youtube", "YouTube", Icons.Outlined.OndemandVideo, Icons.Filled.OndemandVideo)
    object Library : Screen("library", "Library", Icons.Outlined.FolderOpen, Icons.Outlined.FolderOpen)

    // ── Non-tab routes ────────────────────────────────
    object Settings : Screen("settings", "Settings", Icons.Outlined.Home, Icons.Filled.Home)
    object PlaylistDetail : Screen("playlist/{playlistId}", "Playlist", Icons.Outlined.Home, Icons.Filled.Home) {
        const val ARG_ID = "playlistId"
        fun createRoute(playlistId: Long) = "playlist/$playlistId"
    }
}
