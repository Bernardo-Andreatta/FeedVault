package com.example.securegallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video_clips")
data class VideoClip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaItemId: Long,
    val uri: String,
    val startMs: Long,
    val endMs: Long,
    val label: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false
)
