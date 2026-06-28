package com.bernardo.feedvault.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoClipDao {
    @Query("SELECT * FROM video_clips ORDER BY dateCreated DESC")
    fun getAllClips(): Flow<List<VideoClip>>

    @Query("SELECT * FROM video_clips WHERE mediaItemId = :mediaItemId ORDER BY startMs ASC")
    fun getClipsForMedia(mediaItemId: Long): Flow<List<VideoClip>>

    @Query("SELECT * FROM video_clips")
    suspend fun getAllClipsOnce(): List<VideoClip>

    @Query("SELECT * FROM video_clips WHERE mediaItemId = :mediaItemId ORDER BY startMs ASC")
    suspend fun getClipsForMediaOnce(mediaItemId: Long): List<VideoClip>

    @Query("UPDATE video_clips SET tags = :tags WHERE id = :id")
    suspend fun updateTags(id: Long, tags: String)

    @Query("UPDATE video_clips SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("SELECT * FROM video_clips WHERE id = :id")
    suspend fun getClipById(id: Long): VideoClip?

    @Insert
    suspend fun insertClip(clip: VideoClip): Long

    @Delete
    suspend fun deleteClip(clip: VideoClip)
}
