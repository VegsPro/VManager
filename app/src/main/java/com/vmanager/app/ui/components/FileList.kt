package com.vmanager.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vmanager.app.data.FileIconType
import com.vmanager.app.data.FileItem
import com.vmanager.app.ui.theme.AccentCyan
import com.vmanager.app.ui.theme.BorderGlass
import com.vmanager.app.ui.theme.OnSurfaceVariant
import com.vmanager.app.ui.theme.SurfaceGlass

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileList(
    files: List<FileItem>,
    selected: Set<String>,
    isLoading: Boolean,
    onItemClick: (FileItem) -> Unit,
    onItemLongClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AccentCyan
            )
        } else if (files.isEmpty()) {
            Text(
                text = "Empty folder",
                color = OnSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(files, key = { it.path }) { item ->
                    FileListItem(
                        item = item,
                        isSelected = item.path in selected,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val background = if (isSelected) AccentCyan.copy(alpha = 0.25f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = iconFor(item.iconType),
            contentDescription = null,
            tint = when (item.iconType) {
                FileIconType.FOLDER -> AccentCyan
                FileIconType.IMAGE -> Color(0xFF4ADE80)
                FileIconType.VIDEO -> Color(0xFFF472B6)
                FileIconType.AUDIO -> Color(0xFFA78BFA)
                FileIconType.ARCHIVE -> Color(0xFFFBBF24)
                FileIconType.APK -> Color(0xFF34D399)
                else -> OnSurfaceVariant
            },
            modifier = Modifier.size(28.dp)
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row {
                if (!item.isDirectory) {
                    Text(
                        text = item.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = item.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Selected",
                tint = AccentCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun iconFor(type: FileIconType): ImageVector = when (type) {
    FileIconType.FOLDER -> Icons.Filled.Folder
    FileIconType.IMAGE -> Icons.Filled.Image
    FileIconType.VIDEO -> Icons.Filled.VideoFile
    FileIconType.AUDIO -> Icons.Filled.AudioFile
    FileIconType.PDF -> Icons.Filled.PictureAsPdf
    FileIconType.ARCHIVE -> Icons.Filled.FolderZip
    FileIconType.APK -> Icons.Filled.Android
    FileIconType.TEXT -> Icons.Filled.Description
    FileIconType.GENERIC -> Icons.Filled.InsertDriveFile
}
