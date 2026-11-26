package com.vmanager.app.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified(),
    val extension: String = if (file.isFile) file.extension.lowercase() else "",
    val isHidden: Boolean = file.isHidden || name.startsWith(".")
) {
    val formattedSize: String
        get() = when {
            isDirectory -> ""
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> String.format("%.1f KB", size / 1024.0)
            size < 1024 * 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024))
            else -> String.format("%.2f GB", size / (1024.0 * 1024 * 1024))
        }

    val formattedDate: String
        get() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(lastModified))

    fun nameWithoutExt(): String = if (extension.isNotEmpty()) name.removeSuffix(".$extension") else name

    val iconType: FileIconType
        get() = when {
            isDirectory -> FileIconType.FOLDER
            extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic") -> FileIconType.IMAGE
            extension in listOf("mp4", "mkv", "avi", "mov", "webm", "3gp") -> FileIconType.VIDEO
            extension in listOf("mp3", "flac", "wav", "aac", "ogg", "m4a") -> FileIconType.AUDIO
            extension in listOf("pdf") -> FileIconType.PDF
            extension in listOf("zip", "rar", "7z", "tar", "gz", "bz2") -> FileIconType.ARCHIVE
            extension in listOf("apk") -> FileIconType.APK
            extension in listOf("txt", "md", "log", "json", "xml", "html", "css", "js", "kt", "java", "py") -> FileIconType.TEXT
            else -> FileIconType.GENERIC
        }
}

enum class FileIconType {
    FOLDER, IMAGE, VIDEO, AUDIO, PDF, ARCHIVE, APK, TEXT, GENERIC
}

data class TabInfo(
    val id: String,
    val path: String,
    val title: String = File(path).name.ifEmpty { path }
)

data class Bookmark(
    val id: String,
    val path: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

enum class OperationType {
    COPY, MOVE, DELETE, RENAME, NEW_FOLDER, EXTRACT, COMPRESS
}

data class FileOperation(
    val id: String,
    val type: OperationType,
    val sources: List<String>,
    val destination: String? = null,
    val newName: String? = null,
    var progress: Float = 0f,
    var status: OperationStatus = OperationStatus.PENDING,
    var error: String? = null
)

enum class OperationStatus {
    PENDING, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
}
