package com.example.securegallery.ui

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.securegallery.data.DesktopFile
import com.example.securegallery.data.DesktopRepository
import com.example.securegallery.data.DesktopStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DesktopConnectionState { IDLE, CONNECTING, CONNECTED, ERROR }
enum class DesktopFileFilter { ALL, IMAGES, VIDEOS }

data class DesktopUiState(
    val address: String = "",
    val connectionState: DesktopConnectionState = DesktopConnectionState.IDLE,
    val status: DesktopStatus? = null,
    val files: List<DesktopFile> = emptyList(),
    val filter: DesktopFileFilter = DesktopFileFilter.ALL,
    val isLoadingFiles: Boolean = false,
    val downloadingId: String? = null,
    val error: String? = null,
    val selectedIds: Set<String> = emptySet()
) {
    val baseUrl get() = "http://$address"
    val isSelectionMode get() = selectedIds.isNotEmpty()
    val filteredFiles
        get() = when (filter) {
            DesktopFileFilter.ALL -> files
            DesktopFileFilter.IMAGES -> files.filter { it.isImage }
            DesktopFileFilter.VIDEOS -> files.filter { it.isVideo }
        }
    val selectedFiles get() = filteredFiles.filter { it.id in selectedIds }
}

class DesktopViewModel(private val context: Context) : ViewModel() {
    private val prefs = context.getSharedPreferences("desktop_prefs", Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(
        DesktopUiState(address = prefs.getString("server_address", "") ?: "")
    )
    val uiState: StateFlow<DesktopUiState> = _uiState.asStateFlow()

    fun setAddress(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }

    fun connect() {
        val address = _uiState.value.address.trim()
        if (address.isBlank()) return
        prefs.edit().putString("server_address", address).apply()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionState = DesktopConnectionState.CONNECTING,
                error = null
            )
            runCatching {
                val status = DesktopRepository.fetchStatus(_uiState.value.baseUrl)
                _uiState.value = _uiState.value.copy(
                    connectionState = DesktopConnectionState.CONNECTED,
                    status = status
                )
                loadFiles()
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    connectionState = DesktopConnectionState.ERROR,
                    error = "Não foi possível conectar: ${e.message}"
                )
            }
        }
    }

    fun disconnect() {
        _uiState.value = _uiState.value.copy(
            connectionState = DesktopConnectionState.IDLE,
            status = null,
            files = emptyList(),
            error = null
        )
    }

    fun refresh() {
        viewModelScope.launch { loadFiles() }
    }

    fun setFilter(filter: DesktopFileFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun toggleSelection(id: String) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (id in current) current - id else current + id
        )
    }

    fun selectAll() {
        _uiState.value = _uiState.value.copy(
            selectedIds = _uiState.value.filteredFiles.map { it.id }.toSet()
        )
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedIds = emptySet())
    }

    fun setDownloadingId(id: String?) {
        _uiState.value = _uiState.value.copy(downloadingId = id)
    }

    fun getDownloadFolderUri(): Uri? {
        val s = prefs.getString("desktop_folder_uri", null) ?: return null
        return Uri.parse(s)
    }

    fun getDownloadFolderName(): String? {
        val uri = getDownloadFolderUri() ?: return null
        return runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
    }

    fun setDownloadFolderUri(uriStr: String) {
        prefs.edit().putString("desktop_folder_uri", uriStr).apply()
    }

    private suspend fun loadFiles() {
        _uiState.value = _uiState.value.copy(isLoadingFiles = true)
        runCatching {
            val files = DesktopRepository.fetchFiles(_uiState.value.baseUrl)
            _uiState.value = _uiState.value.copy(files = files)
        }.onFailure { e ->
            _uiState.value = _uiState.value.copy(error = "Erro ao listar arquivos: ${e.message}")
        }
        _uiState.value = _uiState.value.copy(isLoadingFiles = false)
    }
}
