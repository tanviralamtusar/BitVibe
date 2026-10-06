package com.bitvibe.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.TextGrey
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionWrapper(content: @Composable () -> Unit) {
    val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission)

    if (permissionState.status.isGranted) {
        content()
    } else {
        PermissionRequestScreen(
            onRequestPermission = { permissionState.launchPermissionRequest() }
        )
    }
}

@Composable
private fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.MusicNote,
                contentDescription = null,
                tint = BitVibeCyan,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "BitVibe",
                style = MaterialTheme.typography.displayLarge,
                color = BitVibeCyan
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your premium music player",
                style = MaterialTheme.typography.bodyLarge,
                color = TextGrey
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "BitVibe needs access to your music files to play your favorite tracks.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BitVibeCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Grant Permission",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
