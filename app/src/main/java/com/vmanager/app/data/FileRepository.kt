package com.vmanager.app.data

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class FileRepository(private val context: Context) {

    fun getRootPaths(): List<File> {
        val roots = mutableListOf<File>()
        roots.add(Environment.getExternalStorageDirectory())
        roots.add(context.filesDir)
        roots.add(context.cacheDir)
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
        if (path.startsWith("zip://")) {
            val parts = path.removePrefix("zip://").split("!", limit = 2)
            val zipPath = parts[0]
            return@withContext listZipEntries(File(zipPath))
        }
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
        result.sortedBy { !it.isDirectory }
    }

    suspend fun listZipEntries(zipFile: File): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val entries = mutableListOf<FileItem>()
            ZipFile(zipFile).use { zf ->
                val topLevel = mutableSetOf<String>()
                zf.entries().asSequence().forEach { entry ->
                    val name = entry.name.trimEnd('/')
                    val first = name.substringBefore("/")
                    if (first.isNotEmpty()) topLevel.add(first)
                }
                topLevel.forEach { name ->
                    val isDir = zf.getEntry("$name/") != null || zf.entries().asSequence().any { it.name.startsWith("$name/") }
                    val size = zf.getEntry(name)?.size ?: 0L
                    entries.add(
                        FileItem(
                            file = File(zipFile.parentFile, name),
                            name = name,
                            path = "zip://${zipFile.absolutePath}!$name",
                            isDirectory = isDir,
                            size = size,
                            lastModified = zipFile.lastModified()
                        )
                    )
                }
            }
            entries.sortedBy { !it.isDirectory }.sortedBy { it.name.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun copyFiles(
        sources: List<File>,
        destDir: File,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val total = sources.size.toFloat().coerceAtLeast(1f)
            sources.forEachIndexed { index, src ->
                val dest = File(destDir, src.name)
                if (src.isDirectory) copyDirectory(src, dest) else src.copyTo(dest, overwrite = true)
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
            if (child.isDirectory) copyDirectory(child, target) else child.copyTo(target, overwrite = true)
        }
    }

    suspend fun moveFiles(
        sources: List<File>,
        destDir: File,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val total = sources.size.toFloat().coerceAtLeast(1f)
            sources.forEachIndexed { index, src ->
                val dest = File(destDir, src.name)
                if (!src.renameTo(dest)) {
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
            val total = files.size.toFloat().coerceAtLeast(1f)
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
            if (file.renameTo(dest)) Result.success(dest) else Result.failure(Exception("Rename failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun batchRename(
        files: List<File>,
        pattern: String,
        replacement: String,
        useRegex: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            files.forEachIndexed { index, file ->
                val newName = if (useRegex) {
                    try {
                        Regex(pattern).replace(file.name, replacement)
                    } catch (e: Exception) {
                        file.name
                    }
                } else {
                    var result = replacement
                    val base = file.nameWithoutExtension
                    val ext = file.extension
                    result = result.replace("*", base).replace("{name}", base)
                    result = result.replace("{ext}", ext)
                    result = result.replace("#", (index + 1).toString())
                    if (ext.isNotEmpty() && !result.endsWith(".$ext")) result += ".$ext"
                    result
                }
                if (newName != file.name && newName.isNotBlank()) {
                    val dest = File(file.parent, newName)
                    if (!dest.exists() && file.renameTo(dest)) count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFolder(parent: File, name: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val folder = File(parent, name)
            if (folder.mkdirs()) Result.success(folder) else Result.failure(Exception("Cannot create folder"))
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
                children.forEach { child -> addToZip(child, "$entryName/${child.name}", zos) }
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
                    if (entry.isDirectory) outFile.mkdirs()
                    else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                    count++
                    onProgress((count % 50) / 50f)
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
