package com.bernardo.feedvault.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.bernardo.feedvault.vault.VaultManager
import com.bernardo.feedvault.vault.VaultSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MediaRepository(
    private val context: Context,
    private val mediaItemDao: MediaItemDao,
    private val videoClipDao: VideoClipDao
) {
    private val prefs = context.getSharedPreferences("gallery_prefs", android.content.Context.MODE_PRIVATE)
    fun getAllClips(): Flow<List<VideoClip>> = videoClipDao.getAllClips()
    fun getClipsForMedia(mediaItemId: Long): Flow<List<VideoClip>> = videoClipDao.getClipsForMedia(mediaItemId)
    suspend fun insertClip(clip: VideoClip): Long = videoClipDao.insertClip(clip)
    suspend fun deleteClip(clip: VideoClip) = videoClipDao.deleteClip(clip)
    suspend fun updateClipTags(clipId: Long, tags: List<String>) =
        videoClipDao.updateTags(clipId, tags.joinToString(","))
    suspend fun toggleClipFavorite(clipId: Long) = withContext(Dispatchers.IO) {
        val clip = videoClipDao.getClipById(clipId) ?: return@withContext
        videoClipDao.updateFavorite(clipId, !clip.isFavorite)
    }
    fun getAllMediaItems(): Flow<List<MediaItem>> = mediaItemDao.getAllMediaItems()

    fun getMediaByType(type: String?): Flow<List<MediaItem>> = mediaItemDao.getMediaByType(type)

    fun getAllTags(): Flow<List<String>> = mediaItemDao.getAllTags()

    fun getAllPeople(): Flow<List<String>> = mediaItemDao.getAllPeople()

    suspend fun insertMediaItem(item: MediaItem) = mediaItemDao.insertMediaItem(item)

    suspend fun updateMediaItem(item: MediaItem) = mediaItemDao.updateMediaItem(item)

    suspend fun deleteMediaItem(item: MediaItem) = mediaItemDao.deleteMediaItem(item)

    /** Encrypts [uri] into the vault, deletes the original, and records a vault:// MediaItem. */
    suspend fun importToVault(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            // Capture the original's mtime before importUri() deletes it, so we can purge
            // the now-dead non-encrypted DB row (the "ghost") if the file was already in the gallery.
            val originalModified = queryLastModified(uri)
            val vi = VaultManager.importUri(context, uri)
            val storedUri = VaultSession.uriFor(vi.storedFileName)
            // If the file was already in the gallery (a ghost row exists), remember its folder
            // so Restore can put it back in the same place.
            val ghost = if (originalModified > 0)
                mediaItemDao.getMediaByFileNameAndModified(vi.displayName, originalModified) else null
            val originFolder = ghost?.let { getSavedFolderUri()?.toString() }
            mediaItemDao.insertMediaItem(
                MediaItem(
                    uri = storedUri,
                    fileName = vi.displayName,
                    uriHash = storedUri.hashCode(),
                    mediaType = vi.mediaType,
                    mimeType = vi.mimeType,
                    aspectRatio = vi.aspectRatio,
                    lastModified = System.currentTimeMillis(),
                    dateAdded = System.currentTimeMillis(),
                    encrypted = true,
                    originFolderUri = originFolder
                )
            )
            // Drop the original's stale gallery entry immediately (no rescan needed).
            mediaItemDao.deleteByUri(uri.toString())
            if (originalModified > 0) {
                mediaItemDao.findUnencryptedByNameModified(vi.displayName, originalModified)
                    .forEach { mediaItemDao.deleteMediaItem(it) }
            }
            VaultSession.register(listOf(mediaItemDao.getMediaByUri(storedUri) ?: return@withContext true))
            true
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "importToVault failed", e)
            false
        }
    }

    private fun queryLastModified(uri: Uri): Long = try {
        context.contentResolver.query(
            uri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null
        )?.use { c -> if (c.moveToFirst()) c.getLong(0) else 0L } ?: 0L
    } catch (e: Exception) { 0L }

    /**
     * Restores a vault item. If its origin folder is known and writable, it goes back there
     * (and reappears in the gallery); otherwise it falls back to the system gallery (Pictures/Movies).
     */
    suspend fun restoreFromVault(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        if (!item.encrypted) return@withContext false
        val name = VaultSession.nameOf(item.uri)
        val origin = item.originFolderUri
        val ok = if (origin != null) restoreToFolder(item, name, Uri.parse(origin))
                 else VaultManager.restoreBlobToGallery(context, name, item.fileName, item.mediaType, item.mimeType)
        if (ok) {
            videoClipDao.getClipsForMediaOnce(item.id).forEach { videoClipDao.deleteClip(it) }
            mediaItemDao.deleteMediaItem(item)
            VaultSession.forget(name)
        }
        ok
    }

    /** Writes a decrypted blob back into [folderUri] via SAF and re-registers it in the gallery. */
    private suspend fun restoreToFolder(item: MediaItem, storedName: String, folderUri: Uri): Boolean {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
        if (folder == null || !folder.canWrite()) {
            // Origin folder no longer accessible — fall back to the system gallery.
            return VaultManager.restoreBlobToGallery(context, storedName, item.fileName, item.mediaType, item.mimeType)
        }
        val mime = item.mimeType.ifBlank { "application/octet-stream" }
        val doc = folder.createFile(mime, item.fileName) ?: return false
        val wrote = context.contentResolver.openOutputStream(doc.uri)?.use { out ->
            VaultManager.decryptBlobToStream(context, storedName, out)
        } ?: false
        if (!wrote) {
            runCatching { doc.delete() }
            return false
        }
        // Normalize to a tree document uri so it matches what the folder scan produces.
        val uriStr = runCatching {
            val docId = DocumentsContract.getDocumentId(doc.uri)
            DocumentsContract.buildDocumentUriUsingTree(folderUri, docId).toString()
        }.getOrElse { doc.uri.toString() }
        if (mediaItemDao.getMediaByUri(uriStr) == null) {
            mediaItemDao.insertMediaItem(
                MediaItem(
                    uri = uriStr,
                    fileName = item.fileName,
                    uriHash = uriStr.hashCode(),
                    mediaType = item.mediaType,
                    mimeType = item.mimeType,
                    aspectRatio = item.aspectRatio,
                    lastModified = System.currentTimeMillis(),
                    dateAdded = System.currentTimeMillis(),
                    encrypted = false
                )
            )
        }
        VaultManager.deleteBlobByName(context, storedName)
        return true
    }

    suspend fun deleteMediaFile(item: MediaItem) = withContext(Dispatchers.IO) {
        if (item.encrypted) {
            VaultManager.deleteBlobByName(context, VaultSession.nameOf(item.uri))
            VaultSession.forget(VaultSession.nameOf(item.uri))
            videoClipDao.getClipsForMediaOnce(item.id).forEach { videoClipDao.deleteClip(it) }
            mediaItemDao.deleteMediaItem(item)
            return@withContext
        }
        val uri = Uri.parse(item.uri)
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (e: Throwable) {
            android.util.Log.w("MediaRepository", "deleteMediaFile failed: ${e.message}")
        }
        videoClipDao.getClipsForMediaOnce(item.id).forEach { videoClipDao.deleteClip(it) }
        mediaItemDao.deleteMediaItem(item)
    }

    suspend fun scanMediaFromFolder(folderUri: Uri) = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(folderUri)
        val mediasFound = mutableListOf<MediaItem>()
        val foundUris = mutableSetOf<String>()
        val jsonCandidates = mutableListOf<Pair<String, Long>>() // docId → lastModified
        scanDirectory(treeUri = folderUri, docId = rootDocId, personName = null, results = mediasFound, foundUris = foundUris, jsonCandidates = jsonCandidates)
        if (mediasFound.isNotEmpty()) {
            mediaItemDao.insertAll(mediasFound)
        }
        // Remove DB items from this folder tree that no longer exist on disk
        val treePrefix = folderUri.toString() + "/document/"
        mediaItemDao.getAllMediaItemsOnce()
            .filter { it.uri.startsWith(treePrefix) && it.uri !in foundUris }
            .forEach { mediaItemDao.deleteMediaItem(it) }
        // Auto-import tags from most recently modified secure_folder JSON found in the tree
        jsonCandidates.maxByOrNull { it.second }?.let { (docId, _) ->
            val jsonUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
            val json = context.contentResolver.openInputStream(jsonUri)?.bufferedReader()?.use { it.readText() }
            if (!json.isNullOrBlank()) importTagData(json)
        }
        mediasFound
    }

    private suspend fun scanDirectory(
        treeUri: Uri,
        docId: String,
        personName: String?,
        noPerson: Boolean = false,
        results: MutableList<MediaItem>,
        foundUris: MutableSet<String> = mutableSetOf(),
        jsonCandidates: MutableList<Pair<String, Long>> = mutableListOf()
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext()) {
                val childDocId = cursor.getString(idIdx)
                val name = cursor.getString(nameIdx)
                val mime = cursor.getString(mimeIdx)
                val modified = cursor.getLong(modIdx)

                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        val isRestantes = name.equals("restantes", ignoreCase = true)
                        scanDirectory(
                            treeUri = treeUri,
                            docId = childDocId,
                            personName = if (noPerson || isRestantes) null else (personName ?: name),
                            noPerson = noPerson || isRestantes,
                            results = results,
                            foundUris = foundUris,
                            jsonCandidates = jsonCandidates
                        )
                    }
                    (mime == "application/json" || name.endsWith(".json", ignoreCase = true))
                        && name.contains("secure_gallery", ignoreCase = true) -> {
                        jsonCandidates.add(childDocId to modified)
                    }
                    mime?.startsWith("image/") == true || mime?.startsWith("video/") == true -> {
                        val mediaUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childDocId)
                        val uriStr = mediaUri.toString()
                        foundUris.add(uriStr)
                        if (mediaItemDao.getMediaByUri(uriStr) == null &&
                            mediaItemDao.getMediaByFileNameAndModified(name, modified) == null) {
                            results.add(
                                MediaItem(
                                    uri = uriStr,
                                    fileName = name,
                                    uriHash = uriStr.hashCode(),
                                    mediaType = if (mime.startsWith("image/")) "image" else "video",
                                    mimeType = mime,
                                    aspectRatio = if (mime.startsWith("video/")) computeVideoAspectRatio(mediaUri) else 0f,
                                    lastModified = modified,
                                    dateAdded = System.currentTimeMillis(),
                                    people = if (!noPerson && personName != null) listOf(personName) else emptyList()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    suspend fun addMediaFiles(uris: List<Uri>) = withContext(Dispatchers.IO) {
        uris.forEach { uri ->
            val uriStr = uri.toString()
            if (mediaItemDao.getMediaByUri(uriStr) != null) return@forEach
            val mime = context.contentResolver.getType(uri) ?: return@forEach
            if (!mime.startsWith("image/") && !mime.startsWith("video/")) return@forEach
            var displayName = uri.lastPathSegment ?: "unknown"
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) displayName = cursor.getString(idx)
                }
            }
            mediaItemDao.insertMediaItem(
                MediaItem(
                    uri = uriStr,
                    fileName = displayName,
                    uriHash = uriStr.hashCode(),
                    mediaType = if (mime.startsWith("image/")) "image" else "video",
                    mimeType = mime,
                    aspectRatio = if (mime.startsWith("video/")) computeVideoAspectRatio(uri) else 0f,
                    lastModified = System.currentTimeMillis(),
                    dateAdded = System.currentTimeMillis(),
                    people = emptyList()
                )
            )
        }
    }

    suspend fun getMediaItemById(id: Long): MediaItem? = mediaItemDao.getMediaItemById(id)

    suspend fun updateTags(itemId: Long, tags: List<String>) = withContext(Dispatchers.IO) {
        val item = mediaItemDao.getMediaItemById(itemId) ?: return@withContext
        mediaItemDao.updateMediaItem(item.copy(tags = tags))
    }

    suspend fun updatePeople(itemId: Long, people: List<String>) = withContext(Dispatchers.IO) {
        val item = mediaItemDao.getMediaItemById(itemId) ?: return@withContext
        mediaItemDao.updateMediaItem(item.copy(people = people))
    }

    suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        val item = mediaItemDao.getMediaItemById(id) ?: return@withContext
        mediaItemDao.updateMediaItem(item.copy(isFavorite = !item.isFavorite))
    }

    suspend fun updateThumbnailFrameMs(itemId: Long, frameMs: Long) = withContext(Dispatchers.IO) {
        mediaItemDao.updateThumbnailFrameMs(itemId, frameMs)
    }

    suspend fun precomputeMissingAspectRatios() = withContext(Dispatchers.IO) {
        mediaItemDao.getVideosWithUnknownAspectRatio().forEach { item ->
            val ratio = computeVideoAspectRatio(Uri.parse(item.uri))
            if (ratio > 0f) mediaItemDao.updateAspectRatio(item.id, ratio)
        }
    }

    suspend fun filterMedia(
        people: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        mediaType: String? = null
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val allMedia = when {
            mediaType != null -> mediaItemDao.getMediaByType(mediaType)
            else -> mediaItemDao.getAllMediaItems()
        }

        // Será usado com Flow, então retornar empty list aqui como fallback
        emptyList()
    }

    suspend fun exportTagData(): String = withContext(Dispatchers.IO) {
        val allClips = videoClipDao.getAllClipsOnce()
        val clipsByMediaId = allClips.groupBy { it.mediaItemId }
        val items = mediaItemDao.getAllMediaItemsOnce()
            .filter { it.tags.isNotEmpty() || it.people.isNotEmpty() || it.isFavorite || it.thumbnailFrameMs >= 0 || clipsByMediaId.containsKey(it.id) }
        val array = org.json.JSONArray()
        for (item in items) {
            val obj = org.json.JSONObject().apply {
                put("fileName", item.fileName)
                put("lastModified", item.lastModified)
                put("tags", org.json.JSONArray(item.tags))
                put("people", org.json.JSONArray(item.people))
                put("isFavorite", item.isFavorite)
                if (item.thumbnailFrameMs >= 0) put("thumbnailFrameMs", item.thumbnailFrameMs)
                val clipsArr = org.json.JSONArray()
                clipsByMediaId[item.id]?.forEach { clip ->
                    clipsArr.put(org.json.JSONObject().apply {
                        put("startMs", clip.startMs)
                        put("endMs", clip.endMs)
                        put("label", clip.label)
                        put("dateCreated", clip.dateCreated)
                        put("tags", org.json.JSONArray(clip.tags))
                        put("isFavorite", clip.isFavorite)
                    })
                }
                put("clips", clipsArr)
            }
            array.put(obj)
        }
        val peopleOrder = prefs.getString("people_order", null)
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val root = org.json.JSONObject().apply {
            put("version", 2)
            put("peopleOrder", org.json.JSONArray(peopleOrder))
            put("items", array)
        }
        root.toString(2)
    }

    suspend fun importTagData(json: String): Int = withContext(Dispatchers.IO) {
        val trimmed = json.trim()
        val root = if (trimmed.startsWith("{")) org.json.JSONObject(trimmed) else null
        val array = if (root != null) root.getJSONArray("items") else org.json.JSONArray(trimmed)
        if (root != null) {
            val orderArr = root.optJSONArray("peopleOrder")
            if (orderArr != null && orderArr.length() > 0) {
                val order = (0 until orderArr.length()).map { orderArr.getString(it) }
                prefs.edit().putString("people_order", order.joinToString(",")).apply()
            }
        }
        var updated = 0
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val fileName = obj.getString("fileName")
            val lastModified = obj.getLong("lastModified")
            val tagsArr = obj.optJSONArray("tags")
            val peopleArr = obj.optJSONArray("people")
            val isFavorite = obj.optBoolean("isFavorite", false)
            val thumbnailFrameMs = obj.optLong("thumbnailFrameMs", -1L)
            val clipsArr = obj.optJSONArray("clips")
            val tags = (0 until (tagsArr?.length() ?: 0)).map { tagsArr!!.getString(it) }
            val people = (0 until (peopleArr?.length() ?: 0)).map { peopleArr!!.getString(it) }
            val item = mediaItemDao.getMediaByFileNameAndModified(fileName, lastModified)
            if (item != null) {
                mediaItemDao.updateMediaItem(item.copy(tags = tags, people = people, isFavorite = isFavorite, thumbnailFrameMs = thumbnailFrameMs))
                updated++
                if (clipsArr != null) {
                    val existing = videoClipDao.getClipsForMediaOnce(item.id)
                    for (j in 0 until clipsArr.length()) {
                        val c = clipsArr.getJSONObject(j)
                        val startMs = c.getLong("startMs")
                        val endMs = c.getLong("endMs")
                        if (existing.none { it.startMs == startMs && it.endMs == endMs }) {
                            val clipTagsArr = c.optJSONArray("tags")
                            val clipTags = (0 until (clipTagsArr?.length() ?: 0)).map { clipTagsArr!!.getString(it) }
                            videoClipDao.insertClip(VideoClip(
                                mediaItemId = item.id,
                                uri = item.uri,
                                startMs = startMs,
                                endMs = endMs,
                                label = c.optString("label", ""),
                                dateCreated = c.optLong("dateCreated", System.currentTimeMillis()),
                                tags = clipTags,
                                isFavorite = c.optBoolean("isFavorite", false)
                            ))
                        }
                    }
                }
            }
        }
        updated
    }

    private fun computeVideoAspectRatio(uri: Uri): Float {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (w > 0 && h > 0) {
                val raw = if (rot == 90 || rot == 270) h.toFloat() / w.toFloat() else w.toFloat() / h.toFloat()
                raw.coerceIn(0.2f, 5f)
            } else 0f
        } catch (e: Exception) { 0f }
        finally { runCatching { r.release() } }
    }

    fun getSavedFolderUri(): Uri? {
        val uriStr = prefs.getString("folder_uri", null) ?: return null
        return Uri.parse(uriStr)
    }

    suspend fun saveMediaFromUrl(downloadUrl: String, fileName: String, targetFolderUri: Uri, tags: List<String> = emptyList(), people: List<String> = emptyList(), onProgress: ((Float) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, targetFolderUri) ?: return@withContext false
        val ext = fileName.substringAfterLast('.', "jpg").lowercase()
        val mime = when (ext) {
            "mp4" -> "video/mp4"
            "webm" -> "video/webm"
            "gif" -> "image/gif"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        val docFile = folder.createFile(mime, fileName) ?: return@withContext false
        try {
            context.contentResolver.openOutputStream(docFile.uri)?.use { out ->
                val conn = java.net.URL(downloadUrl).openConnection() as java.net.HttpURLConnection
                conn.setRequestProperty("User-Agent", "FeedVault/1.0")
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15_000
                conn.readTimeout = 60_000
                try {
                    val code = conn.responseCode
                    android.util.Log.d("MediaRepository", "download $downloadUrl → $code")
                    if (code !in 200..299) throw java.io.IOException("HTTP $code")
                    val contentLength = conn.contentLengthLong
                    conn.inputStream.use { input ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead = 0L
                        var n: Int
                        while (input.read(buffer).also { n = it } != -1) {
                            out.write(buffer, 0, n)
                            bytesRead += n
                            if (contentLength > 0) onProgress?.invoke(bytesRead.toFloat() / contentLength.toFloat())
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            }
            // Normalize to tree document URI so it matches what scanDirectory produces
            val uriStr = runCatching {
                val docId = DocumentsContract.getDocumentId(docFile.uri)
                DocumentsContract.buildDocumentUriUsingTree(targetFolderUri, docId).toString()
            }.getOrElse { docFile.uri.toString() }
            val isVideo = mime.startsWith("video/")
            if (mediaItemDao.getMediaByUri(uriStr) == null) {
                mediaItemDao.insertMediaItem(
                    MediaItem(
                        uri = uriStr,
                        fileName = fileName,
                        uriHash = uriStr.hashCode(),
                        mediaType = if (isVideo) "video" else "image",
                        mimeType = mime,
                        aspectRatio = if (isVideo) computeVideoAspectRatio(docFile.uri) else 0f,
                        lastModified = System.currentTimeMillis(),
                        dateAdded = System.currentTimeMillis(),
                        tags = tags,
                        people = people
                    )
                )
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "saveMediaFromUrl failed", e)
            runCatching { docFile.delete() }
            false
        }
    }

    suspend fun scanFromMediaStore(): Int = withContext(Dispatchers.IO) {
        val collections = buildList {
            add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI to "image")
            add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI to "video")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Downloads.EXTERNAL_CONTENT_URI to null) // may contain both
            }
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        var inserted = 0
        for ((collectionUri, defaultType) in collections) {
            runCatching {
                context.contentResolver.query(collectionUri, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val modCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: continue
                        val mime = cursor.getString(mimeCol) ?: continue
                        val modified = cursor.getLong(modCol) * 1000L
                        val mediaType = when {
                            mime.startsWith("image/") -> "image"
                            mime.startsWith("video/") -> "video"
                            defaultType != null -> defaultType
                            else -> continue
                        }
                        val contentUri = when {
                            mime.startsWith("image/") -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            mime.startsWith("video/") -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                            else -> continue
                        }
                        val itemUri = ContentUris.withAppendedId(contentUri, id).toString()
                        if (mediaItemDao.getMediaByUri(itemUri) == null &&
                            mediaItemDao.getMediaByFileNameAndModified(name, modified) == null) {
                            mediaItemDao.insertMediaItem(
                                MediaItem(
                                    uri = itemUri,
                                    fileName = name,
                                    uriHash = itemUri.hashCode(),
                                    mediaType = mediaType,
                                    mimeType = mime,
                                    aspectRatio = if (mediaType == "video") computeVideoAspectRatio(Uri.parse(itemUri)) else 0f,
                                    lastModified = modified,
                                    dateAdded = System.currentTimeMillis()
                                )
                            )
                            inserted++
                        }
                    }
                }
            }
        }
        inserted
    }
}
