package com.vmanager.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vmanager.app.ui.theme.AccentCyan
import com.vmanager.app.ui.theme.OnSurfaceVariant

@Composable
fun PaneToolbar(
    currentPath: String,
    selectedCount: Int,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Path bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateUp) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Up", tint = AccentCyan)
            }
            Text(
                text = currentPath,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = OnSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentCyan)
            }
        }

        // Action bar when selection active
        if (selectedCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentCyan
                )
                Row {
                    TextButton(onClick = onSelectAll) {
                        Text("All", color = AccentCyan)
                    }
                    TextButton(onClick = onClearSelection) {
                        Text("Clear", color = AccentCyan)
                    }
                }
            }
        }
    }
}
