package com.example.securegallery.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Metadata for one media file stored encrypted inside the app's private storage.
 *
 * The encrypted bytes live on disk at [storedFileName] (full file) and
 * "[storedFileName].thumb" (small preview). Only metadata is kept in the DB —
 * never the plaintext bytes. Encryption is byte-exact (AES-GCM over the raw
 * stream), so a restored file is identical to the original: no quality loss.
 */
@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Random UUID-based name of the encrypted blob in the vault directory. */
    val storedFileName: String,
    /** Original display name, shown in the UI and used when restoring. */
    val displayName: String,
    /** "image" or "video". GIFs are "image" with mimeType image/gif. */
    val mediaType: String,
    val mimeType: String,
    val sizeBytes: Long = 0,
    val aspectRatio: Float = 0f,
    val dateAdded: Long = System.currentTimeMillis()
)
