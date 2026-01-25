package com.bitvibe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import com.bitvibe.app.ui.theme.BitVibeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.navigation.compose.composable

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
    val navController = androidx.navigation.compose.rememberNavController()
    var showPlayerScreen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

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
            androidx.navigation.compose.NavHost(
                navController = navController,
                startDestination = com.bitvibe.app.ui.navigation.Screen.Library.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(com.bitvibe.app.ui.navigation.Screen.Library.route) {
                    com.bitvibe.app.ui.screens.library.LibraryScreen()
                }
                composable(com.bitvibe.app.ui.navigation.Screen.Folders.route) {
                    com.bitvibe.app.ui.screens.folders.FoldersScreen()
                }
                composable(com.bitvibe.app.ui.navigation.Screen.Playlists.route) {
                    com.bitvibe.app.ui.screens.playlists.PlaylistsScreen()
                }
                composable(com.bitvibe.app.ui.navigation.Screen.Settings.route) {
                    com.bitvibe.app.ui.screens.settings.SettingsScreen(
                        onMusicFoldersClick = {
                            navController.navigate(com.bitvibe.app.ui.navigation.Screen.Folders.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BitVibeTheme {
        Greeting("Android")
    }
}