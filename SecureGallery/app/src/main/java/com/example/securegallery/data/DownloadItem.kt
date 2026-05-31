package com.example.securegallery.data

import android.net.Uri
import java.util.UUID

enum class DownloadStatus { QUEUED, DOWNLOADING, DONE, FAILED }

data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val source: String,
    val url: String,
    val fileName: String,
    val folderUri: Uri,
    val tags: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val error: String? = null,
    val addedAt: Long = System.currentTimeMillis()
)
