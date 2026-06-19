package com.example.securegallery.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class DesktopFile(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val folderId: String
) {
    val isVideo get() = mimeType.startsWith("video/")
    val isImage get() = mimeType.startsWith("image/")
    val ext get() = name.substringAfterLast('.', "bin").lowercase()
}

data class DesktopStatus(
    val name: String,
    val version: String,
    val ip: String,
    val fileCount: Int,
    val folderCount: Int
)

object DesktopRepository {

    fun downloadUrl(baseUrl: String, fileId: String) = "$baseUrl/api/files/$fileId/download"
    fun thumbnailUrl(baseUrl: String, fileId: String) = "$baseUrl/api/files/$fileId/thumbnail"

    suspend fun fetchStatus(baseUrl: String): DesktopStatus = withContext(Dispatchers.IO) {
        val json = get("$baseUrl/api/status")
        val o = JSONObject(json)
        DesktopStatus(
            name = o.optString("name"),
            version = o.optString("version"),
            ip = o.optString("ip"),
            fileCount = o.optInt("file_count"),
            folderCount = o.optInt("folder_count")
        )
    }

    suspend fun fetchFiles(baseUrl: String): List<DesktopFile> = withContext(Dispatchers.IO) {
        val json = get("$baseUrl/api/files")
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            DesktopFile(
                id = o.getString("id"),
                name = o.getString("name"),
                size = o.getLong("size"),
                mimeType = o.optString("mimeType", "application/octet-stream"),
                folderId = o.optString("folderId", "")
            )
        }
    }

    private fun get(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 5_000
        conn.readTimeout = 10_000
        return try {
            Log.d("DesktopRepo", "GET $urlStr → ${conn.responseCode}")
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            stream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
