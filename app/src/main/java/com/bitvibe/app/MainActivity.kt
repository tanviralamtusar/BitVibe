package com.bitvibe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bitvibe.app.data.repository.SettingsRepository
import com.bitvibe.app.ui.navigation.Screen
import com.bitvibe.app.ui.theme.BitVibeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var musicController: com.bitvibe.app.domain.player.MusicController

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.themeMode
                .collectAsStateWithLifecycle(initialValue = SettingsRepository.THEME_DARK)
            BitVibeTheme(amoled = themeMode == SettingsRepository.THEME_BLACK) {
                com.bitvibe.app.ui.common.PermissionWrapper {
                    MainApp(musicController)
                }
            }
        }
    }
}

@Composable
fun MainApp(musicController: com.bitvibe.app.domain.player.MusicController) {
    val navController = rememberNavController()
    var showPlayerScreen by rememberSaveable { mutableStateOf(false) }

    if (showPlayerScreen) {
        // System back collapses the full player instead of leaving the app.
        BackHandler { showPlayerScreen = false }
        com.bitvibe.app.ui.player.PlayerScreen(
            musicController = musicController,
            onCollapse = { showPlayerScreen = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                Column {
                    com.bitvibe.app.ui.player.MiniPlayer(
                        musicController = musicController,
                        onExpand = { showPlayerScreen = true }
                    )
                    com.bitvibe.app.ui.navigation.BottomNavigationBar(navController = navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    com.bitvibe.app.ui.screens.home.HomeScreen(
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }
                composable(Screen.Explore.route) {
                    com.bitvibe.app.ui.screens.explore.ExploreScreen()
                }
                composable(Screen.Library.route) {
                    com.bitvibe.app.ui.screens.library.LibraryScreen(
                        onPlaylistClick = { playlistId ->
                            navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                        }
                    )
                }
                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(navArgument(Screen.PlaylistDetail.ARG_ID) { type = NavType.LongType })
                ) { entry ->
                    com.bitvibe.app.ui.screens.playlists.PlaylistDetailScreen(
                        playlistId = entry.arguments?.getLong(Screen.PlaylistDetail.ARG_ID) ?: 0L,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Settings.route) {
                    com.bitvibe.app.ui.screens.settings.SettingsScreen()
                }
            }
        }
    }
}
