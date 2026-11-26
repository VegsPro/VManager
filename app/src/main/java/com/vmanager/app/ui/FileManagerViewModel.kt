package com.vmanager.app.ui

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vmanager.app.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class PaneState(
    val currentPath: String = Environment.getExternalStorageDirectory().absolutePath,
    val files: List<FileItem> = emptyList(),
    val selected: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showHidden: Boolean = false,
    val sortBy: SortBy = SortBy.NAME,
    val ascending: Boolean = true
)

data class FileManagerUiState(
    val leftPane: PaneState = PaneState(),
    val rightPane: PaneState = PaneState(),
    val dualPane: Boolean = true,
    val activePane: PaneSide = PaneSide.LEFT,
    val tabs: List<TabInfo> = emptyList(),
    val activeTabId: String? = null,
    val bookmarks: List<Bookmark> = emptyList(),
    val operations: List<FileOperation> = emptyList(),
    val glassEnabled: Boolean = true,
    val showQueue: Boolean = false,
    val showBookmarks: Boolean = false,
    val showSettings: Boolean = false,
    val storageStats: StorageStats? = null
)

enum class PaneSide { LEFT, RIGHT }

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FileRepository(application)

    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    init {
        val root = Environment.getExternalStorageDirectory().absolutePath
        val initialTab = TabInfo(UUID.randomUUID().toString(), root, "Internal")
        _uiState.update {
            it.copy(
                tabs = listOf(initialTab),
                activeTabId = initialTab.id,
                leftPane = it.leftPane.copy(currentPath = root),
                rightPane = it.rightPane.copy(currentPath = root)
            )
        }
        refreshPane(PaneSide.LEFT)
        refreshPane(PaneSide.RIGHT)
        loadStorageStats()
        loadDefaultBookmarks()
    }

    private fun loadDefaultBookmarks() {
        val defaults = listOf(
            Bookmark("bm_internal", Environment.getExternalStorageDirectory().absolutePath, "Internal Storage"),
            Bookmark("bm_download", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath, "Downloads"),
            Bookmark("bm_dcim", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath, "DCIM"),
            Bookmark("bm_pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath, "Pictures"),
            Bookmark("bm_documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath, "Documents")
        )
        _uiState.update { it.copy(bookmarks = defaults) }
    }

    fun loadStorageStats() {
        viewModelScope.launch {
            val stats = repo.getStorageStats()
            _uiState.update { it.copy(storageStats = stats) }
        }
    }

    fun refreshPane(side: PaneSide) {
        viewModelScope.launch {
            val pane = if (side == PaneSide.LEFT) _uiState.value.leftPane else _uiState.value.rightPane
            updatePane(side) { it.copy(isLoading = true, error = null) }
            try {
                val files = repo.listFiles(
                    pane.currentPath,
                    pane.showHidden,
                    pane.sortBy,
                    pane.ascending
                )
                updatePane(side) { it.copy(files = files, isLoading = false, selected = emptySet()) }
            } catch (e: Exception) {
                updatePane(side) { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun updatePane(side: PaneSide, transform: (PaneState) -> PaneState) {
        _uiState.update { state ->
            if (side == PaneSide.LEFT) state.copy(leftPane = transform(state.leftPane))
            else state.copy(rightPane = transform(state.rightPane))
        }
    }

    fun navigateTo(side: PaneSide, path: String) {
        updatePane(side) { it.copy(currentPath = path, selected = emptySet()) }
        refreshPane(side)
        // Update active tab title if needed
        _uiState.update { state ->
            val tabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) tab.copy(path = path, title = File(path).name.ifEmpty { "Root" })
                else tab
            }
            state.copy(tabs = tabs)
        }
    }

    fun navigateUp(side: PaneSide) {
        val pane = if (side == PaneSide.LEFT) _uiState.value.leftPane else _uiState.value.rightPane
        val parent = File(pane.currentPath).parentFile
        if (parent != null && parent.canRead()) {
            navigateTo(side, parent.absolutePath)
        }
    }

    fun toggleSelection(side: PaneSide, path: String) {
        updatePane(side) { pane ->
            val newSelected = if (path in pane.selected) pane.selected - path else pane.selected + path
            pane.copy(selected = newSelected)
        }
    }

    fun selectAll(side: PaneSide) {
        updatePane(side) { pane ->
            pane.copy(selected = pane.files.map { it.path }.toSet())
        }
    }

    fun clearSelection(side: PaneSide) {
        updatePane(side) { it.copy(selected = emptySet()) }
    }

    fun setActivePane(side: PaneSide) {
        _uiState.update { it.copy(activePane = side) }
    }

    fun toggleDualPane() {
        _uiState.update { it.copy(dualPane = !it.dualPane) }
    }

    fun toggleGlass() {
        _uiState.update { it.copy(glassEnabled = !it.glassEnabled) }
    }

    fun setShowHidden(side: PaneSide, show: Boolean) {
        updatePane(side) { it.copy(showHidden = show) }
        refreshPane(side)
    }

    fun setSort(side: PaneSide, sortBy: SortBy, ascending: Boolean = true) {
        updatePane(side) { it.copy(sortBy = sortBy, ascending = ascending) }
        refreshPane(side)
    }

    // Operations
    fun copySelected(from: PaneSide, to: PaneSide) {
        val state = _uiState.value
        val sourcePane = if (from == PaneSide.LEFT) state.leftPane else state.rightPane
        val destPane = if (to == PaneSide.LEFT) state.leftPane else state.rightPane
        val sources = sourcePane.files.filter { it.path in sourcePane.selected }.map { it.file }
        if (sources.isEmpty()) return

        val op = FileOperation(
            id = UUID.randomUUID().toString(),
            type = OperationType.COPY,
            sources = sources.map { it.absolutePath },
            destination = destPane.currentPath
        )
        enqueueOperation(op)
    }

    fun moveSelected(from: PaneSide, to: PaneSide) {
        val state = _uiState.value
        val sourcePane = if (from == PaneSide.LEFT) state.leftPane else state.rightPane
        val destPane = if (to == PaneSide.LEFT) state.leftPane else state.rightPane
        val sources = sourcePane.files.filter { it.path in sourcePane.selected }.map { it.file }
        if (sources.isEmpty()) return

        val op = FileOperation(
            id = UUID.randomUUID().toString(),
            type = OperationType.MOVE,
            sources = sources.map { it.absolutePath },
            destination = destPane.currentPath
        )
        enqueueOperation(op)
    }

    fun deleteSelected(side: PaneSide) {
        val pane = if (side == PaneSide.LEFT) _uiState.value.leftPane else _uiState.value.rightPane
        val sources = pane.files.filter { it.path in pane.selected }.map { it.file }
        if (sources.isEmpty()) return

        val op = FileOperation(
            id = UUID.randomUUID().toString(),
            type = OperationType.DELETE,
            sources = sources.map { it.absolutePath }
        )
        enqueueOperation(op)
    }

    private fun enqueueOperation(op: FileOperation) {
        _uiState.update { it.copy(operations = it.operations + op, showQueue = true) }
        processNextOperation()
    }

    private fun processNextOperation() {
        val next = _uiState.value.operations.firstOrNull { it.status == OperationStatus.PENDING } ?: return
        viewModelScope.launch {
            updateOperation(next.id) { it.copy(status = OperationStatus.RUNNING) }
            val result = when (next.type) {
                OperationType.COPY -> {
                    val sources = next.sources.map { File(it) }
                    val dest = File(next.destination!!)
                    repo.copyFiles(sources, dest) { progress ->
                        updateOperation(next.id) { it.copy(progress = progress) }
                    }
                }
                OperationType.MOVE -> {
                    val sources = next.sources.map { File(it) }
                    val dest = File(next.destination!!)
                    repo.moveFiles(sources, dest) { progress ->
                        updateOperation(next.id) { it.copy(progress = progress) }
                    }
                }
                OperationType.DELETE -> {
                    val sources = next.sources.map { File(it) }
                    repo.deleteFiles(sources) { progress ->
                        updateOperation(next.id) { it.copy(progress = progress) }
                    }
                }
                else -> Result.success(Unit)
            }
            updateOperation(next.id) {
                if (result.isSuccess) it.copy(status = OperationStatus.COMPLETED, progress = 1f)
                else it.copy(status = OperationStatus.FAILED, error = result.exceptionOrNull()?.message)
            }
            // Refresh panes
            refreshPane(PaneSide.LEFT)
            refreshPane(PaneSide.RIGHT)
            processNextOperation()
        }
    }

    private fun updateOperation(id: String, transform: (FileOperation) -> FileOperation) {
        _uiState.update { state ->
            state.copy(operations = state.operations.map { if (it.id == id) transform(it) else it })
        }
    }

    fun cancelOperation(id: String) {
        updateOperation(id) { it.copy(status = OperationStatus.CANCELLED) }
    }

    fun clearCompletedOperations() {
        _uiState.update {
            it.copy(operations = it.operations.filter { op ->
                op.status !in listOf(OperationStatus.COMPLETED, OperationStatus.FAILED, OperationStatus.CANCELLED)
            })
        }
    }

    fun createFolder(side: PaneSide, name: String) {
        viewModelScope.launch {
            val pane = if (side == PaneSide.LEFT) _uiState.value.leftPane else _uiState.value.rightPane
            repo.createFolder(File(pane.currentPath), name)
            refreshPane(side)
        }
    }

    fun renameFile(side: PaneSide, path: String, newName: String) {
        viewModelScope.launch {
            repo.renameFile(File(path), newName)
            refreshPane(side)
        }
    }

    // Tabs
    fun openNewTab(path: String = Environment.getExternalStorageDirectory().absolutePath) {
        val tab = TabInfo(UUID.randomUUID().toString(), path)
        _uiState.update {
            it.copy(tabs = it.tabs + tab, activeTabId = tab.id)
        }
        navigateTo(_uiState.value.activePane, path)
    }

    fun closeTab(tabId: String) {
        _uiState.update { state ->
            val newTabs = state.tabs.filter { it.id != tabId }
            val newActive = if (state.activeTabId == tabId) newTabs.lastOrNull()?.id else state.activeTabId
            state.copy(tabs = newTabs, activeTabId = newActive)
        }
    }

    fun switchTab(tabId: String) {
        val tab = _uiState.value.tabs.find { it.id == tabId } ?: return
        _uiState.update { it.copy(activeTabId = tabId) }
        navigateTo(_uiState.value.activePane, tab.path)
    }

    // Bookmarks
    fun addBookmark(path: String, name: String) {
        val bm = Bookmark(UUID.randomUUID().toString(), path, name)
        _uiState.update { it.copy(bookmarks = it.bookmarks + bm) }
    }

    fun removeBookmark(id: String) {
        _uiState.update { it.copy(bookmarks = it.bookmarks.filter { bm -> bm.id != id }) }
    }

    fun toggleQueue() {
        _uiState.update { it.copy(showQueue = !it.showQueue) }
    }

    fun toggleBookmarks() {
        _uiState.update { it.copy(showBookmarks = !it.showBookmarks) }
    }

    fun toggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun openZipAsFolder(side: PaneSide, zipPath: String) {
        navigateTo(side, "zip://$zipPath")
    }

    fun batchRenameSelected(side: PaneSide, pattern: String, replacement: String, useRegex: Boolean) {
        viewModelScope.launch {
            val pane = if (side == PaneSide.LEFT) _uiState.value.leftPane else _uiState.value.rightPane
            val sources = pane.files.filter { it.path in pane.selected }.map { it.file }
            if (sources.isEmpty()) return@launch
            repo.batchRename(sources, pattern, replacement, useRegex)
            refreshPane(side)
        }
    }

    fun extractSelectedZip(side: PaneSide) {
        viewModelScope.launch {
            val pane = if (side == PaneSide.LEFT) _uiState.value.leftPane else _uiState.value.rightPane
            val zips = pane.files.filter { it.path in pane.selected && it.extension == "zip" }
            zips.forEach { item ->
                val dest = File(pane.currentPath, item.nameWithoutExt())
                repo.extractZip(item.file, dest)
            }
            refreshPane(side)
        }
    }

}
