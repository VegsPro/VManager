@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vmanager.app.ui.screens



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vmanager.app.data.FileItem
import com.vmanager.app.ui.FileManagerViewModel
import com.vmanager.app.ui.PaneSide
import com.vmanager.app.ui.components.*
import com.vmanager.app.ui.theme.*



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FileManagerViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<FileItem?>(null) }
    var newFolderName by remember { mutableStateOf("") }
    var renameName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BackgroundDark, BackgroundDark.copy(alpha = 0.95f), BackgroundDark)
                )
            )
            
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        "VManager",
                        style = MaterialTheme.typography.titleLarge,
                        color = AccentCyan
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleDualPane() }) {
                        Icon(
                            if (state.dualPane) Icons.Default.ViewColumn else Icons.Default.ViewAgenda,
                            contentDescription = "Toggle dual pane",
                            tint = AccentCyan
                        )
                    }
                    IconButton(onClick = { viewModel.toggleBookmarks() }) {
                        Icon(Icons.Default.Bookmark, contentDescription = "Bookmarks", tint = AccentCyan)
                    }
                    IconButton(onClick = { viewModel.toggleQueue() }) {
                        BadgedBox(badge = {
                            if (state.operations.any { it.status == com.vmanager.app.data.OperationStatus.RUNNING || it.status == com.vmanager.app.data.OperationStatus.PENDING }) {
                                Badge { Text("${state.operations.count { it.status == com.vmanager.app.data.OperationStatus.RUNNING || it.status == com.vmanager.app.data.OperationStatus.PENDING }}") }
                            }
                        }) {
                            Icon(Icons.Default.Queue, contentDescription = "Queue", tint = AccentCyan)
                        }
                    }
                    IconButton(onClick = { viewModel.toggleSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = AccentCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceGlassStrong.copy(alpha = 0.6f)
                )
            )

            // Tabs row
            if (state.tabs.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = state.tabs.indexOfFirst { it.id == state.activeTabId }.coerceAtLeast(0),
                    containerColor = SurfaceGlass.copy(alpha = 0.3f),
                    edgePadding = 8.dp,
                    divider = {}
                ) {
                    state.tabs.forEach { tab ->
                        Tab(
                            selected = tab.id == state.activeTabId,
                            onClick = { viewModel.switchTab(tab.id) },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        tab.title,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    if (state.tabs.size > 1) {
                                        IconButton(
                                            onClick = { viewModel.closeTab(tab.id) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        )
                    }
                    // New tab button
                    IconButton(onClick = { viewModel.openNewTab() }) {
                        Icon(Icons.Default.Add, contentDescription = "New tab", tint = AccentCyan)
                    }
                }
            }

            // Dual / Single pane content
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                // Left pane
                PaneContent(
                    pane = state.leftPane,
                    side = PaneSide.LEFT,
                    isActive = state.activePane == PaneSide.LEFT,
                    dualPane = state.dualPane,
                    onNavigate = { viewModel.navigateTo(PaneSide.LEFT, it) },
                    onNavigateUp = { viewModel.navigateUp(PaneSide.LEFT) },
                    onRefresh = { viewModel.refreshPane(PaneSide.LEFT) },
                    onSelect = { viewModel.toggleSelection(PaneSide.LEFT, it.path) },
                    onSelectAll = { viewModel.selectAll(PaneSide.LEFT) },
                    onClearSelection = { viewModel.clearSelection(PaneSide.LEFT) },
                    onItemClick = { item ->
                        if (state.leftPane.selected.isNotEmpty()) {
                            viewModel.toggleSelection(PaneSide.LEFT, item.path)
                        } else if (item.isDirectory) {
                            viewModel.navigateTo(PaneSide.LEFT, item.path)
                        } else if (item.extension == "zip") {
                            viewModel.openZipAsFolder(PaneSide.LEFT, item.path)
                        }
                    },
                    onItemLongClick = { viewModel.toggleSelection(PaneSide.LEFT, it.path) },
                    onNewFolder = { showNewFolderDialog = true; viewModel.setActivePane(PaneSide.LEFT) },
                    modifier = Modifier.weight(1f)
                )

                if (state.dualPane) {
                    // Divider with transfer buttons
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IconButton(
                            onClick = { viewModel.copySelected(PaneSide.LEFT, PaneSide.RIGHT) },
                            enabled = state.leftPane.selected.isNotEmpty()
                        ) {
                            Icon(Icons.Default.ContentCopy, "Copy →", tint = AccentCyan)
                        }
                        IconButton(
                            onClick = { viewModel.moveSelected(PaneSide.LEFT, PaneSide.RIGHT) },
                            enabled = state.leftPane.selected.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DriveFileMove, "Move →", tint = AccentCyan)
                        }
                        IconButton(
                            onClick = { viewModel.copySelected(PaneSide.RIGHT, PaneSide.LEFT) },
                            enabled = state.rightPane.selected.isNotEmpty()
                        ) {
                            Icon(Icons.Default.ContentCopy, "← Copy", tint = AccentCyan)
                        }
                        IconButton(
                            onClick = { viewModel.moveSelected(PaneSide.RIGHT, PaneSide.LEFT) },
                            enabled = state.rightPane.selected.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DriveFileMove, "← Move", tint = AccentCyan)
                        }
                        IconButton(
                            onClick = { viewModel.deleteSelected(state.activePane) },
                            enabled = (if (state.activePane == PaneSide.LEFT) state.leftPane.selected else state.rightPane.selected).isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, "Delete", tint = ErrorRed)
                        }
                    }

                    // Right pane
                    PaneContent(
                        pane = state.rightPane,
                        side = PaneSide.RIGHT,
                        isActive = state.activePane == PaneSide.RIGHT,
                        dualPane = true,
                        onNavigate = { viewModel.navigateTo(PaneSide.RIGHT, it) },
                        onNavigateUp = { viewModel.navigateUp(PaneSide.RIGHT) },
                        onRefresh = { viewModel.refreshPane(PaneSide.RIGHT) },
                        onSelect = { viewModel.toggleSelection(PaneSide.RIGHT, it.path) },
                        onSelectAll = { viewModel.selectAll(PaneSide.RIGHT) },
                        onClearSelection = { viewModel.clearSelection(PaneSide.RIGHT) },
                        onItemClick = { item ->
                            if (state.rightPane.selected.isNotEmpty()) {
                                viewModel.toggleSelection(PaneSide.RIGHT, item.path)
                            } else if (item.isDirectory) {
                                viewModel.navigateTo(PaneSide.RIGHT, item.path)
                            } else if (item.extension == "zip") {
                                viewModel.openZipAsFolder(PaneSide.RIGHT, item.path)
                            }
                        },
                        onItemLongClick = { viewModel.toggleSelection(PaneSide.RIGHT, it.path) },
                        onNewFolder = { showNewFolderDialog = true; viewModel.setActivePane(PaneSide.RIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom status bar
            state.storageStats?.let { stats ->
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { stats.usedPercent },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AccentCyan,
                            trackColor = BorderGlass
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "${formatSize(stats.usedBytes)} / ${formatSize(stats.totalBytes)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }
        }

        // Dialogs & sheets
        if (showNewFolderDialog) {
            AlertDialog(
                onDismissRequest = { showNewFolderDialog = false },
                title = { Text("New Folder") },
                text = {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(state.activePane, newFolderName)
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showNewFolderDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (state.showQueue) {
            OperationsQueueSheet(
                operations = state.operations,
                onDismiss = { viewModel.toggleQueue() },
                onCancel = { viewModel.cancelOperation(it) },
                onClearCompleted = { viewModel.clearCompletedOperations() }
            )
        }

        if (state.showBookmarks) {
            BookmarksSheet(
                bookmarks = state.bookmarks,
                onDismiss = { viewModel.toggleBookmarks() },
                onOpen = { path ->
                    viewModel.navigateTo(state.activePane, path)
                    viewModel.toggleBookmarks()
                },
                onRemove = { viewModel.removeBookmark(it) },
                onAddCurrent = {
                    val pane = if (state.activePane == PaneSide.LEFT) state.leftPane else state.rightPane
                    viewModel.addBookmark(pane.currentPath, java.io.File(pane.currentPath).name)
                }
            )
        }

        if (state.showSettings) {
            SettingsSheet(
                glassEnabled = state.glassEnabled,
                onToggleGlass = { viewModel.toggleGlass() },
                onDismiss = { viewModel.toggleSettings() }
            )
        }
    }
}

@Composable
private fun PaneContent(
    pane: com.vmanager.app.ui.PaneState,
    side: PaneSide,
    isActive: Boolean,
    dualPane: Boolean,
    onNavigate: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onSelect: (FileItem) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp),
        content = {
            Column(modifier = Modifier.fillMaxSize()) {
                PaneToolbar(
                    currentPath = pane.currentPath,
                    selectedCount = pane.selected.size,
                    onNavigateUp = onNavigateUp,
                    onRefresh = onRefresh,
                    onSelectAll = onSelectAll,
                    onClearSelection = onClearSelection,
                    onNewFolder = onNewFolder
                )
                HorizontalDivider(color = BorderGlass.copy(alpha = 0.3f))
                FileList(
                    files = pane.files,
                    selected = pane.selected,
                    isLoading = pane.isLoading,
                    onItemClick = onItemClick,
                    onItemLongClick = onItemLongClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    )

}

@Composable
private fun OperationsQueueSheet(
    operations: List<com.vmanager.app.data.FileOperation>,
    onDismiss: () -> Unit,
    onCancel: (String) -> Unit,
    onClearCompleted: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Operations Queue", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
            Spacer(Modifier.height(12.dp))
            if (operations.isEmpty()) {
                Text("No operations", color = OnSurfaceVariant)
            } else {
                operations.forEach { op ->
                    GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${op.type} — ${op.sources.size} items", style = MaterialTheme.typography.bodyMedium)
                        LinearProgressIndicator(
                            progress = { op.progress },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            color = AccentCyan
                        )
                        Text(
                            text = op.status.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = when (op.status) {
                                com.vmanager.app.data.OperationStatus.COMPLETED -> SuccessGreen
                                com.vmanager.app.data.OperationStatus.FAILED -> ErrorRed
                                com.vmanager.app.data.OperationStatus.RUNNING -> AccentCyan
                                else -> OnSurfaceVariant
                            }
                        )
                    }
                }
                TextButton(onClick = onClearCompleted) {
                    Text("Clear completed")
                }
            }
        }
    }
}

@Composable
private fun BookmarksSheet(
    bookmarks: List<com.vmanager.app.data.Bookmark>,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onAddCurrent: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bookmarks", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
                TextButton(onClick = onAddCurrent) { Text("Add current") }
            }
            Spacer(Modifier.height(8.dp))
            bookmarks.forEach { bm ->
                ListItem(
                    headlineContent = { Text(bm.name) },
                    supportingContent = { Text(bm.path, maxLines = 1) },
                    leadingContent = { Icon(Icons.Default.Bookmark, null, tint = AccentCyan) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(bm.id) }) {
                            Icon(Icons.Default.Delete, null, tint = ErrorRed)
                        }
                    },
                    modifier = Modifier.clickable { onOpen(bm.path) }
                )
            }
        }
    }
}

@Composable
private fun SettingsSheet(
    glassEnabled: Boolean,
    onToggleGlass: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = AccentCyan)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Glass Effect")
                GlassSwitch(checked = glassEnabled, onCheckedChange = { onToggleGlass() })
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "VManager 1.0.0\nAdvanced dual-pane file manager\nwith Glass UI",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
