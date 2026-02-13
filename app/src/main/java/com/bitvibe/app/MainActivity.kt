package com.bitvibe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bitvibe.app.ui.theme.BitVibeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var musicController: com.bitvibe.app.domain.player.MusicController

    @Inject
    lateinit var settingsRepository: com.bitvibe.app.data.repository.SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BitVibeTheme {
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
    var showPlayerScreen by remember { mutableStateOf(false) }

    if (showPlayerScreen) {
        com.bitvibe.app.ui.player.PlayerScreen(
            musicController = musicController,
            onCollapse = { showPlayerScreen = false },
            onSettingsClick = {
                showPlayerScreen = false
                navController.navigate(com.bitvibe.app.ui.navigation.Screen.Settings.route)
            }
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
                startDestination = com.bitvibe.app.ui.navigation.Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(com.bitvibe.app.ui.navigation.Screen.Home.route) {
                    com.bitvibe.app.ui.screens.home.HomeScreen(
                        onSettingsClick = {
                            navController.navigate(com.bitvibe.app.ui.navigation.Screen.Settings.route)
                        }
                    )
                }
                composable(com.bitvibe.app.ui.navigation.Screen.Explore.route) {
                    com.bitvibe.app.ui.screens.explore.ExploreScreen()
                }
                composable(com.bitvibe.app.ui.navigation.Screen.Library.route) {
                    com.bitvibe.app.ui.screens.library.LibraryScreen()
                }
                composable(com.bitvibe.app.ui.navigation.Screen.Settings.route) {
                    com.bitvibe.app.ui.screens.settings.SettingsScreen(
                        onMusicFoldersClick = { /* no-op, folders integrated into library */ }
                    )
                }
            }
        }
    }
}