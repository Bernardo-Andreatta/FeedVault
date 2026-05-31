package com.example.securegallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "media_items")
@TypeConverters(Converters::class)
data class MediaItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val fileName: String,
    val uriHash: Int,
    val mediaType: String, // "image" ou "video"
    val mimeType: String = "",
    val dateAdded: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val people: List<String> = emptyList(),
    val notes: String = "",
    val isFavorite: Boolean = false,
    val aspectRatio: Float = 0f,
    val lastModified: Long = System.currentTimeMillis(),
    val thumbnailFrameMs: Long = -1L
)
