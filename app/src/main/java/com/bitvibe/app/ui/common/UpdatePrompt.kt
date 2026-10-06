package com.bitvibe.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitvibe.app.data.update.AvailableUpdate
import com.bitvibe.app.data.update.UpdateManager
import com.bitvibe.app.data.update.UpdateState
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey

/**
 * Shows update dialogs only when the user is needed: an update to confirm, the one-time
 * "install unknown apps" permission, a user-started download, or an error. Silent updates
 * (Android 12+, after the first self-update) never show anything.
 */
@Composable
fun UpdatePrompt(updateManager: UpdateManager) {
    val state by updateManager.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // "Later" on a downloaded update hides it until the next launch.
    var hiddenState by remember { mutableStateOf<UpdateState?>(null) }
    if (state == hiddenState) return

    when (val s = state) {
        is UpdateState.Available -> UpdateDialog(
            title = "Update available",
            update = s.update,
            confirmLabel = "Update now",
            onConfirm = { updateManager.downloadAndInstall(s.update) },
            onLater = { updateManager.skip(s.update) }
        )

        is UpdateState.Ready -> if (!s.silent) {
            UpdateDialog(
                title = "Update ready",
                update = s.update,
                confirmLabel = "Install",
                onConfirm = { updateManager.installReady() },
                onLater = { hiddenState = s }
            )
        }

        is UpdateState.Downloading -> if (s.userInitiated) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                title = { Text("Downloading update", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        LinearProgressIndicator(
                            progress = { s.progress },
                            color = BitVibeCyan,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(s.progress * 100).toInt()}%", color = TextGrey)
                    }
                },
                confirmButton = {}
            )
        }

        is UpdateState.NeedsPermission -> AlertDialog(
            onDismissRequest = { hiddenState = s },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Allow updates", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "To install updates, allow BitVibe to install apps. You only need to do this once; " +
                        "come back to BitVibe afterwards and the update continues.",
                    color = TextGrey
                )
            },
            confirmButton = {
                TextButton(onClick = { context.startActivity(updateManager.unknownSourcesSettingsIntent()) }) {
                    Text("Open settings", color = BitVibeCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { hiddenState = s }) { Text("Later", color = TextGrey) }
            }
        )

        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = { updateManager.dismiss() },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text("Update failed", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(s.message, color = TextGrey) },
            confirmButton = {
                TextButton(onClick = { updateManager.dismiss() }) { Text("OK", color = BitVibeCyan) }
            }
        )

        else -> Unit
    }
}

@Composable
private fun UpdateDialog(
    title: String,
    update: AvailableUpdate,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "BitVibe ${update.versionName} (build ${update.build})",
                    style = MaterialTheme.typography.titleSmall,
                    color = BitVibeCyan
                )
                if (update.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(update.notes, style = MaterialTheme.typography.bodySmall, color = TextGrey)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your playlists and settings are kept. BitVibe closes while it installs; open it again afterwards.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGrey
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel, color = BitVibeCyan) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("Later", color = TextGrey) }
        }
    )
}
