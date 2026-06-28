package com.bernardo.feedvault.vault

import android.content.Context
import android.net.Uri
import com.bernardo.feedvault.data.MediaItem
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges the unified gallery to encrypted storage. Encrypted [MediaItem]s carry
 * a stable "vault://<storedName>" uri; everywhere the app turns a media uri into
 * a real [Uri] for Coil / ExoPlayer / MediaMetadataRetriever it calls
 * [resolve], which returns a decrypted temp-file uri while the vault is unlocked.
 *
 * On unlock the whole set is decrypted in the background ([decryptAll]) so
 * [resolve] is a cheap map lookup; on lock the temp files are wiped ([clear]).
 */
object VaultSession {

    const val PREFIX = "vault://"

    private lateinit var appContext: Context
    private val mimeByName = ConcurrentHashMap<String, String>()
    private val tempByName = ConcurrentHashMap<String, File>()

    fun init(context: Context) { appContext = context.applicationContext }

    fun isVault(uriString: String): Boolean = uriString.startsWith(PREFIX)

    fun nameOf(uriString: String): String = uriString.removePrefix(PREFIX)

    fun uriFor(storedName: String): String = PREFIX + storedName

    /** Keep the name→mime map current with the database. */
    fun register(items: List<MediaItem>) {
        items.forEach { if (it.encrypted) mimeByName[nameOf(it.uri)] = it.mimeType }
    }

    fun resolve(uriString: String): Uri {
        if (!isVault(uriString)) return Uri.parse(uriString)
        if (!::appContext.isInitialized) return Uri.parse(uriString)
        val name = nameOf(uriString)
        val file = tempByName[name] ?: run {
            val mime = mimeByName[name] ?: return Uri.parse(uriString)
            VaultManager.decryptBlobToTempSync(appContext, name, mime)?.also { tempByName[name] = it }
        }
        return if (file != null) Uri.fromFile(file) else Uri.parse(uriString)
    }

    /** Eagerly decrypt all registered blobs so playback never blocks on the main thread. */
    suspend fun decryptAll() {
        if (!::appContext.isInitialized) return
        mimeByName.entries.toList().forEach { (name, mime) ->
            if (!tempByName.containsKey(name)) {
                VaultManager.decryptBlobToTempSync(appContext, name, mime)?.let { tempByName[name] = it }
            }
        }
    }

    fun forget(storedName: String) {
        tempByName.remove(storedName)?.let { runCatching { it.delete() } }
        mimeByName.remove(storedName)
    }

    fun clear() {
        tempByName.values.forEach { runCatching { it.delete() } }
        tempByName.clear()
    }
}
