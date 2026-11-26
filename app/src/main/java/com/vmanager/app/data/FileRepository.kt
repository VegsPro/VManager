package com.vmanager.app.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileRepository(private val context: Context) {

    fun getRootPaths(): List<File> {
        val roots = mutableListOf<File>()
        roots.add(Environment.getExternalStorageDirectory())
        roots.add(context.filesDir)
        roots.add(context.cacheDir)
        // Secondary storage if available
        context.getExternalFilesDirs(null).forEach { dir ->
            dir?.parentFile?.parentFile?.let { if (it.exists() && !roots.contains(it)) roots.add(it) }
        }
        return roots.filter { it.exists() && it.canRead() }
    }

    suspend fun listFiles(
        path: String,
        showHidden: Boolean = false,
        sortBy: SortBy = SortBy.NAME,
        ascending: Boolean = true
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory || !dir.canRead()) return@withContext emptyList()

        val files = dir.listFiles()?.map { FileItem(it) } ?: emptyList()
        val filtered = if (showHidden) files else files.filter { !it.isHidden }

        val sorted = when (sortBy) {
            SortBy.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortBy.SIZE -> filtered.sortedBy { it.size }
            SortBy.DATE -> filtered.sortedBy { it.lastModified }
            SortBy.TYPE -> filtered.sortedBy { it.extension }
        }

        val result = if (ascending) sorted else sorted.reversed()
        // Directories first
        result.sortedBy { !it.isDirectory }
    }

    suspend fun copyFiles(
        sources: List<File>,
        destDir: File,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val total = sources.size.toFloat()
            sources.forEachIndexed { index, src ->
                val dest = File(destDir, src.name)
                if (src.isDirectory) {
                    copyDirectory(src, dest)
                } else {
                    src.copyTo(dest, overwrite = true)
                }
                onProgress((index + 1) / total)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun copyDirectory(src: File, dest: File) {
        if (!dest.exists()) dest.mkdirs()
        src.listFiles()?.forEach { child ->
            val target = File(dest, child.name)
            if (child.isDirectory) copyDirectory(child, target)
            else child.copyTo(target, overwrite = true)
        }
    }

    suspend fun moveFiles(
        sources: List<File>,
        destDir: File,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val total = sources.size.toFloat()
            sources.forEachIndexed { index, src ->
                val dest = File(destDir, src.name)
                if (!src.renameTo(dest)) {
                    // Fallback to copy + delete
                    if (src.isDirectory) copyDirectory(src, dest) else src.copyTo(dest, overwrite = true)
                    src.deleteRecursively()
                }
                onProgress((index + 1) / total)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFiles(
        files: List<File>,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val total = files.size.toFloat()
            files.forEachIndexed { index, file ->
                file.deleteRecursively()
                onProgress((index + 1) / total)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dest = File(file.parent, newName)
            if (file.renameTo(dest)) Result.success(dest)
            else Result.failure(Exception("Rename failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val folder = File(parent, name)
            if (folder.mkdirs()) Result.success(folder)
            else Result.failure(Exception("Cannot create folder"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createZip(
        sources: List<File>,
        zipFile: File,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val total = sources.size.toFloat().coerceAtLeast(1f)
                sources.forEachIndexed { index, file ->
                    addToZip(file, file.name, zos)
                    onProgress((index + 1) / total)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addToZip(file: File, entryName: String, zos: ZipOutputStream) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            if (children.isEmpty()) {
                zos.putNextEntry(ZipEntry("$entryName/"))
                zos.closeEntry()
            } else {
                children.forEach { child ->
                    addToZip(child, "$entryName/${child.name}", zos)
                }
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }

    suspend fun extractZip(
        zipFile: File,
        destDir: File,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!destDir.exists()) destDir.mkdirs()
            ZipInputStream(FileInputStream(zipFile)).use { zis ->
                var entry = zis.nextEntry
                var count = 0
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                    count++
                    onProgress((count % 100) / 100f) // approximate
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getStorageStats(): StorageStats {
        val external = Environment.getExternalStorageDirectory()
        val total = external.totalSpace
        val free = external.freeSpace
        val used = total - free
        return StorageStats(total, used, free)
    }
}

enum class SortBy { NAME, SIZE, DATE, TYPE }

data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
) {
    val usedPercent: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}
