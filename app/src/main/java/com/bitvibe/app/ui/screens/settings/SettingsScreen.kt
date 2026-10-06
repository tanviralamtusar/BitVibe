package com.bitvibe.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitvibe.app.BuildConfig
import com.bitvibe.app.data.repository.SettingsRepository
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showEqDialog by remember { mutableStateOf(false) }

    // BitVibe is a dark-only design; older "System"/"Light" values fall back to Dark.
    val themeLabel = if (themeMode == SettingsRepository.THEME_BLACK) "Black (AMOLED)" else "Dark"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = BitVibeCyan,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings Groups
        SettingsGroup(title = "Appearance & Audio") {
            SettingsItem(
                icon = Icons.Outlined.Palette,
                title = "Theme",
                subtitle = themeLabel,
                onClick = { showThemeDialog = true }
            )
            SettingsItem(
                icon = Icons.Outlined.Equalizer,
                title = "Equalizer",
                subtitle = "Adjust audio output",
                onClick = { showEqDialog = true }
            )
        }

        SettingsGroup(title = "About") {
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "Version",
                subtitle = BuildConfig.VERSION_NAME,
                onClick = { }
            )
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeMode,
            onThemeSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showEqDialog) {
        EqualizerDialog(
            onDismiss = { showEqDialog = false }
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = BitVibeCyan,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextGrey,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextGrey
            )
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: Int,
    onThemeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        SettingsRepository.THEME_DARK to "Dark",
        SettingsRepository.THEME_BLACK to "Black (AMOLED)"
    )
    val selectedTheme = if (currentTheme == SettingsRepository.THEME_BLACK) currentTheme else SettingsRepository.THEME_DARK
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Select Theme", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        },
        text = {
            Column {
                themes.forEach { (mode, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onThemeSelected(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTheme == mode,
                            onClick = { onThemeSelected(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = BitVibeCyan,
                                unselectedColor = TextGrey
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = BitVibeCyan)
            }
        }
    )
}

@Composable
private fun EqualizerDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Equalizer", style = MaterialTheme.typography.headlineSmall, color = Color.White)
        },
        text = {
            Text(
                "Open the player and enable Pro Mode to access the equalizer.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = BitVibeCyan)
            }
        }
    )
}
