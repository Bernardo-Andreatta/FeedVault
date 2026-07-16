package com.bernardo.feedvault.vault

import android.content.Context
import android.net.Uri
import com.bernardo.feedvault.data.MediaItem
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Bridges the unified gallery to the safe folder. Vault [MediaItem]s carry a stable
 * "vault://<storedName>" uri; everywhere the app turns a media uri into a real [Uri]
 * for Coil / ExoPlayer / MediaMetadataRetriever it calls [resolve].
 *
 * Files in the vault are stored unencrypted in app-internal storage (filesDir/vault),
 * so [resolve] is a plain file-uri lookup — no decryption, no temp files. Internal
 * storage is private to the app, hidden from other apps and from a PC over USB/MTP,
 * and encrypted at rest by the OS while the device is locked. Access is gated by the
 * vault password / biometric in the UI.
 */
object VaultSession {

    const val PREFIX = "vault://"

    private lateinit var appContext: Context
    private val mimeByName = ConcurrentHashMap<String, String>()

    fun init(context: Context) { appContext = context.applicationContext }

    fun isVault(uriString: String): Boolean = uriString.startsWith(PREFIX)

    fun nameOf(uriString: String): String = uriString.removePrefix(PREFIX)

    fun uriFor(storedName: String): String = PREFIX + storedName

    /** Keep the name→mime map current with the database. */
    fun register(items: List<MediaItem>) {
        items.forEach { if (it.encrypted) mimeByName[nameOf(it.uri)] = it.mimeType }
    }

    /** Maps a vault:// uri to the file:// uri of its stored blob, or returns it unchanged. */
    fun resolve(uriString: String): Uri {
        if (!isVault(uriString)) return Uri.parse(uriString)
        if (!::appContext.isInitialized) return Uri.parse(uriString)
        val name = nameOf(uriString)
        val mime = mimeByName[name] ?: ""
        val file: File? = VaultManager.decryptBlobToTempSync(appContext, name, mime)
        return if (file != null) Uri.fromFile(file) else Uri.parse(uriString)
    }

    fun forget(storedName: String) {
        mimeByName.remove(storedName)
    }

    fun clear() {
        // Vault blobs are permanent files, not decrypted temporaries — nothing to wipe here.
        // Locking just drops the in-memory key (see VaultManager.lock); files stay in place.
    }
}
