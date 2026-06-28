package com.example.securegallery.vault

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import com.example.securegallery.data.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKey

/**
 * Holds the unlocked vault key in memory and performs all file operations for
 * the safe folder. The Data Encryption Key (DEK) only exists in RAM while the
 * vault is unlocked; [lock] wipes it and clears decrypted temp files.
 */
object VaultManager {

    private const val PREFS = "vault_prefs"
    private const val K_SALT = "salt"
    private const val K_PW_BLOB = "pw_blob"        // iv||wrapped DEK (password path)
    private const val K_BIO_IV = "bio_iv"
    private const val K_BIO_BLOB = "bio_blob"       // wrapped DEK (biometric path)

    @Volatile private var dek: SecretKey? = null

    val isUnlocked: Boolean get() = dek != null

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isInitialized(context: Context): Boolean = prefs(context).contains(K_PW_BLOB)

    fun isBiometricEnabled(context: Context): Boolean =
        prefs(context).contains(K_BIO_BLOB) && VaultCrypto.biometricKeyExists()

    // ── Password setup / unlock ───────────────────────────────────────────────

    fun setupPassword(context: Context, password: CharArray) {
        val salt = VaultCrypto.randomBytes(16)
        val newDek = VaultCrypto.generateDek()
        val kek = VaultCrypto.deriveKey(password, salt)
        val blob = VaultCrypto.aesGcmEncrypt(kek, newDek.encoded)
        prefs(context).edit()
            .putString(K_SALT, b64(salt))
            .putString(K_PW_BLOB, b64(blob))
            .apply()
        dek = newDek
    }

    /** Returns true if the password was correct. */
    fun unlockWithPassword(context: Context, password: CharArray): Boolean {
        val p = prefs(context)
        val salt = p.getString(K_SALT, null)?.let { unb64(it) } ?: return false
        val blob = p.getString(K_PW_BLOB, null)?.let { unb64(it) } ?: return false
        return try {
            val kek = VaultCrypto.deriveKey(password, salt)
            dek = VaultCrypto.secretKeyFromBytes(VaultCrypto.aesGcmDecrypt(kek, blob))
            true
        } catch (e: Exception) {
            false
        }
    }

    fun lock(context: Context) {
        dek = null
        runCatching { tempDir(context).listFiles()?.forEach { it.delete() } }
    }

    // ── Biometric enable / unlock ─────────────────────────────────────────────

    /** Authorised encrypt cipher to pass through BiometricPrompt before enabling. */
    fun biometricEncryptCipher(): Cipher = VaultCrypto.biometricEncryptCipher()

    /** Call after BiometricPrompt succeeds with the encrypt cipher; requires an unlocked vault. */
    fun enableBiometric(context: Context, authorizedCipher: Cipher) {
        val key = dek ?: error("Vault must be unlocked to enable biometric unlock")
        val wrapped = authorizedCipher.doFinal(key.encoded)
        prefs(context).edit()
            .putString(K_BIO_IV, b64(authorizedCipher.iv))
            .putString(K_BIO_BLOB, b64(wrapped))
            .apply()
    }

    fun disableBiometric(context: Context) {
        prefs(context).edit().remove(K_BIO_IV).remove(K_BIO_BLOB).apply()
        VaultCrypto.deleteBiometricKey()
    }

    /** Decrypt cipher to pass through BiometricPrompt for unlocking, or null if unavailable/invalidated. */
    fun biometricDecryptCipher(context: Context): Cipher? {
        val iv = prefs(context).getString(K_BIO_IV, null)?.let { unb64(it) } ?: return null
        return runCatching { VaultCrypto.biometricDecryptCipher(iv) }.getOrNull()
    }

    /** Call after BiometricPrompt succeeds with the decrypt cipher. */
    fun unlockWithBiometric(context: Context, authorizedCipher: Cipher): Boolean {
        val wrapped = prefs(context).getString(K_BIO_BLOB, null)?.let { unb64(it) } ?: return false
        return try {
            dek = VaultCrypto.secretKeyFromBytes(authorizedCipher.doFinal(wrapped))
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Directories ───────────────────────────────────────────────────────────

    private fun vaultDir(context: Context): File =
        File(context.filesDir, "vault").apply { mkdirs() }

    private fun tempDir(context: Context): File =
        File(context.cacheDir, "vault_tmp").apply { mkdirs() }

    private fun blobFile(context: Context, item: VaultItem): File =
        File(vaultDir(context), item.storedFileName)

    private fun thumbFile(context: Context, item: VaultItem): File =
        File(vaultDir(context), item.storedFileName + ".thumb")

    // ── Import: encrypt into vault, then delete the original ───────────────────

    /**
     * Encrypts the media at [uri] into the vault (byte-exact, no quality loss),
     * deletes the original from the device, and returns the metadata to persist.
     * The caller inserts the returned [VaultItem] into the database.
     */
    suspend fun importUri(context: Context, uri: Uri): VaultItem = withContext(Dispatchers.IO) {
        val key = dek ?: error("Vault is locked")
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: "application/octet-stream"
        val mediaType = if (mime.startsWith("video/")) "video" else "image"
        val displayName = queryDisplayName(context, uri)
        val storedName = UUID.randomUUID().toString().replace("-", "")

        val item = VaultItem(
            storedFileName = storedName,
            displayName = displayName,
            mediaType = mediaType,
            mimeType = mime
        )

        // Encrypt the full file as a raw stream — output is bit-for-bit recoverable.
        cr.openInputStream(uri)?.use { input ->
            FileOutputStream(blobFile(context, item)).use { out ->
                VaultCrypto.encryptStream(key, input, out)
            }
        } ?: error("Cannot open $uri")

        val size = blobFile(context, item).length()
        // Aspect ratio for layout; the unified gallery decrypts the full file on view.
        val (_, aspect) = makeThumbnail(context, uri, mediaType)

        deleteOriginal(context, uri)
        item.copy(sizeBytes = size, aspectRatio = aspect)
    }

    // ── Name-based blob ops (used by the unified gallery via vault:// uris) ─────

    private fun blobFileByName(context: Context, storedName: String): File =
        File(vaultDir(context), storedName)

    /** Synchronous decrypt to a temp file (idempotent). Safe to call off the main thread. */
    fun decryptBlobToTempSync(context: Context, storedName: String, mimeType: String): File? {
        val key = dek ?: return null
        val out = File(tempDir(context), storedName + "." + extFor(mimeType))
        if (out.exists() && out.length() > 0) return out
        return runCatching {
            FileInputStream(blobFileByName(context, storedName)).use { input ->
                FileOutputStream(out).use { o -> VaultCrypto.decryptStream(key, input, o) }
            }
            out
        }.getOrNull()
    }

    fun deleteBlobByName(context: Context, storedName: String) {
        runCatching { blobFileByName(context, storedName).delete() }
        runCatching { File(tempDir(context), storedName).delete() }
        tempDir(context).listFiles()?.filter { it.name.startsWith(storedName) }?.forEach { it.delete() }
    }

    /** Decrypts a blob back to the system gallery, then deletes the blob. */
    suspend fun restoreBlobToGallery(
        context: Context, storedName: String, displayName: String, mediaType: String, mimeType: String
    ): Boolean = withContext(Dispatchers.IO) {
        val key = dek ?: return@withContext false
        val cr = context.contentResolver
        val isVideo = mediaType == "video"
        val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                         else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, if (isVideo) "Movies/FeedVault" else "Pictures/FeedVault")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val target = cr.insert(collection, values) ?: return@withContext false
        val ok = runCatching {
            cr.openOutputStream(target)?.use { out ->
                FileInputStream(blobFileByName(context, storedName)).use { input ->
                    VaultCrypto.decryptStream(key, input, out)
                }
            } ?: return@runCatching false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                cr.update(target, values, null, null)
            }
            true
        }.getOrElse {
            runCatching { cr.delete(target, null, null) }
            false
        }
        if (ok) deleteBlobByName(context, storedName)
        ok
    }

    suspend fun decryptThumb(context: Context, item: VaultItem): ByteArray? =
        withContext(Dispatchers.IO) {
            val key = dek ?: return@withContext null
            val f = thumbFile(context, item)
            if (!f.exists()) return@withContext null
            runCatching {
                ByteArrayOutputStream().use { bos ->
                    FileInputStream(f).use { input -> VaultCrypto.decryptStream(key, input, bos) }
                    bos.toByteArray()
                }
            }.getOrNull()
        }

    /** Decrypts the full media to a temp file (cleared on lock) for viewing/playback. */
    suspend fun decryptToTemp(context: Context, item: VaultItem): File? =
        withContext(Dispatchers.IO) {
            val key = dek ?: return@withContext null
            val out = File(tempDir(context), item.storedFileName + "." + extFor(item.mimeType))
            if (out.exists() && out.length() > 0) return@withContext out
            runCatching {
                FileInputStream(blobFile(context, item)).use { input ->
                    FileOutputStream(out).use { o -> VaultCrypto.decryptStream(key, input, o) }
                }
                out
            }.getOrNull()
        }

    /**
     * Restores [item] back to the system gallery (Pictures/Movies), then removes
     * it from the vault. Returns true on success.
     */
    suspend fun restoreToGallery(context: Context, item: VaultItem): Boolean =
        withContext(Dispatchers.IO) {
            val key = dek ?: return@withContext false
            val cr = context.contentResolver
            val isVideo = item.mediaType == "video"
            val collection = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                             else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val dir = if (isVideo) "Movies/FeedVault" else "Pictures/FeedVault"
                    put(MediaStore.MediaColumns.RELATIVE_PATH, dir)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val target = cr.insert(collection, values) ?: return@withContext false
            val ok = runCatching {
                cr.openOutputStream(target)?.use { out ->
                    FileInputStream(blobFile(context, item)).use { input ->
                        VaultCrypto.decryptStream(key, input, out)
                    }
                } ?: return@runCatching false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    cr.update(target, values, null, null)
                }
                true
            }.getOrElse {
                runCatching { cr.delete(target, null, null) }
                false
            }
            if (ok) deleteFiles(context, item)
            ok
        }

    /** Permanently deletes the encrypted blobs for [item]. Caller removes the DB row. */
    fun deleteFiles(context: Context, item: VaultItem) {
        runCatching { blobFile(context, item).delete() }
        runCatching { thumbFile(context, item).delete() }
        runCatching { File(tempDir(context), item.storedFileName + "." + extFor(item.mimeType)).delete() }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun deleteOriginal(context: Context, uri: Uri) {
        runCatching {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(context.contentResolver, uri)
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        var name = uri.lastPathSegment ?: "media"
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx)?.let { name = it }
                }
            }
        }
        return name
    }

    /** Returns (jpegThumbBytes, aspectRatio). */
    private fun makeThumbnail(context: Context, uri: Uri, mediaType: String): Pair<ByteArray?, Float> {
        return try {
            val bmp: Bitmap = (if (mediaType == "video") {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(context, uri)
                    r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } finally {
                    runCatching { r.release() }
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    BitmapFactory.decodeStream(input, null, opts)
                }
            }) ?: return null to 0f
            val aspect = if (bmp.height > 0) bmp.width.toFloat() / bmp.height.toFloat() else 0f
            val scaled = scaleToMax(bmp, 720)
            val bos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, bos)
            if (scaled != bmp) scaled.recycle()
            bmp.recycle()
            bos.toByteArray() to aspect
        } catch (e: Exception) {
            null to 0f
        }
    }

    private fun scaleToMax(src: Bitmap, max: Int): Bitmap {
        val w = src.width; val h = src.height
        if (w <= max && h <= max) return src
        val ratio = w.toFloat() / h.toFloat()
        val (nw, nh) = if (w >= h) max to (max / ratio).toInt() else (max * ratio).toInt() to max
        return Bitmap.createScaledBitmap(src, nw.coerceAtLeast(1), nh.coerceAtLeast(1), true)
    }

    private fun extFor(mime: String): String = when {
        mime.contains("gif") -> "gif"
        mime.contains("png") -> "png"
        mime.contains("webp") -> "webp"
        mime.startsWith("video/mp4") -> "mp4"
        mime.contains("webm") -> "webm"
        mime.startsWith("video/") -> "mp4"
        else -> "jpg"
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)
}
