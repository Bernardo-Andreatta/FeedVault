package com.bernardo.feedvault.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bernardo.feedvault.data.AppDatabase
import com.bernardo.feedvault.data.DownloadItem
import com.bernardo.feedvault.data.DownloadStatus
import com.bernardo.feedvault.data.MediaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DownloadQueueViewModel(context: Context) : ViewModel() {
    private val db = AppDatabase.getDatabase(context)
    private val repository = MediaRepository(context, db.mediaItemDao(), db.videoClipDao())

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    private var processingJob: Job? = null

    fun enqueue(item: DownloadItem) {
        _items.update { it + item }
        ensureProcessing()
    }

    private fun ensureProcessing() {
        if (processingJob?.isActive == true) return
        processingJob = viewModelScope.launch {
            while (true) {
                val next = _items.value.firstOrNull { it.status == DownloadStatus.QUEUED } ?: break
                process(next)
            }
        }
    }

    private suspend fun process(item: DownloadItem) {
        updateItem(item.id) { it.copy(status = DownloadStatus.DOWNLOADING, progress = 0f, error = null) }
        try {
            val ok = repository.saveMediaFromUrl(
                downloadUrl = item.url,
                fileName = item.fileName,
                targetFolderUri = item.folderUri,
                tags = item.tags,
                people = item.people,
                onProgress = { p ->
                    val prev = _items.value.firstOrNull { it.id == item.id }?.progress ?: 0f
                    if (p - prev >= 0.01f || p >= 0.99f) {
                        updateItem(item.id) { it.copy(progress = p) }
                    }
                }
            )
            updateItem(item.id) {
                it.copy(
                    status = if (ok) DownloadStatus.DONE else DownloadStatus.FAILED,
                    progress = if (ok) 1f else it.progress,
                    error = if (!ok) "Falha ao salvar" else null
                )
            }
        } catch (e: Exception) {
            updateItem(item.id) {
                it.copy(status = DownloadStatus.FAILED, error = e.message ?: "Erro desconhecido")
            }
        }
    }

    fun retry(id: String) {
        _items.update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.QUEUED, progress = 0f, error = null) else it }
        }
        ensureProcessing()
    }

    fun dismiss(id: String) {
        _items.update { it.filter { item -> item.id != id } }
    }

    fun dismissCompleted() {
        _items.update { it.filter { item -> item.status != DownloadStatus.DONE } }
    }

    private fun updateItem(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }
}
