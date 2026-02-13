package com.bitvibe.app.ui.screens.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bitvibe.app.ui.theme.BitVibeCyan
import com.bitvibe.app.ui.theme.DarkBg
import com.bitvibe.app.ui.theme.TextGrey

@Composable
fun FoldersScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = BitVibeCyan.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Folders",
                style = MaterialTheme.typography.headlineSmall,
                color = BitVibeCyan
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Coming Soon",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGrey
            )
        }
    }
}
