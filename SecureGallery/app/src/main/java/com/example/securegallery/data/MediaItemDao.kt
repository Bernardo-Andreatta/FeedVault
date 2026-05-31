package com.example.securegallery.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items ORDER BY lastModified DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Query("SELECT * FROM media_items WHERE id = :id")
    suspend fun getMediaItemById(id: Long): MediaItem?

    @Query("""
        SELECT * FROM media_items 
        WHERE (:type IS NULL OR mediaType = :type)
        ORDER BY dateAdded DESC
    """)
    fun getMediaByType(type: String?): Flow<List<MediaItem>>

    @Insert
    suspend fun insertMediaItem(item: MediaItem): Long

    @Insert
    suspend fun insertAll(items: List<MediaItem>)

    @Update
    suspend fun updateMediaItem(item: MediaItem)

    @Delete
    suspend fun deleteMediaItem(item: MediaItem)

    @Query("SELECT DISTINCT tags FROM media_items")
    fun getAllTags(): Flow<List<String>>

    @Query("SELECT DISTINCT people FROM media_items")
    fun getAllPeople(): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE uri = :uri")
    suspend fun getMediaByUri(uri: String): MediaItem?

    @Query("SELECT * FROM media_items WHERE fileName = :fileName AND ABS(lastModified - :lastModified) < 2000 LIMIT 1")
    suspend fun getMediaByFileNameAndModified(fileName: String, lastModified: Long): MediaItem?

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("UPDATE media_items SET aspectRatio = :ratio WHERE id = :id")
    suspend fun updateAspectRatio(id: Long, ratio: Float)

    @Query("UPDATE media_items SET thumbnailFrameMs = :frameMs WHERE id = :id")
    suspend fun updateThumbnailFrameMs(id: Long, frameMs: Long)

    @Query("SELECT * FROM media_items WHERE mediaType = 'video' AND aspectRatio = 0")
    suspend fun getVideosWithUnknownAspectRatio(): List<MediaItem>

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaItemsOnce(): List<MediaItem>
}
