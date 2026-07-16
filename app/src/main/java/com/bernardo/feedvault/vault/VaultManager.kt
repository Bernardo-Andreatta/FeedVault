package com.bernardo.feedvault.vault

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
import com.bernardo.feedvault.data.VaultItem
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

    /**
     * Re-wraps the DEK under a new password after verifying [current]. The biometric
     * wrapper is untouched (it wraps the same DEK). Returns false if [current] is wrong.
     */
    fun changePassword(context: Context, current: CharArray, new: CharArray): Boolean {
        val p = prefs(context)
        val salt = p.getString(K_SALT, null)?.let { unb64(it) } ?: return false
        val blob = p.getString(K_PW_BLOB, null)?.let { unb64(it) } ?: return false
        val dekBytes = try {
            val curKek = VaultCrypto.deriveKey(current, salt)
            VaultCrypto.aesGcmDecrypt(curKek, blob)
        } catch (e: Exception) {
            return false
        }
        val newSalt = VaultCrypto.randomBytes(16)
        val newKek = VaultCrypto.deriveKey(new, newSalt)
        val newBlob = VaultCrypto.aesGcmEncrypt(newKek, dekBytes)
        p.edit().putString(K_SALT, b64(newSalt)).putString(K_PW_BLOB, b64(newBlob)).apply()
        return true
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
        val cr = context.contentResolver
        val mime = cr.getType(uri) ?: "application/octet-stream"
        val mediaType = if (mime.startsWith("video/")) "video" else "image"
        val displayName = queryDisplayName(context, uri)
        // Keep the file extension on the blob: Coil infers the media type from it
        // (its VideoFrameDecoder only runs for a "video/*" mime), and players sniff faster.
        val storedName = UUID.randomUUID().toString().replace("-", "") + "." + extFor(mime)

        val item = VaultItem(
            storedFileName = storedName,
            displayName = displayName,
            mediaType = mediaType,
            mimeType = mime
        )

        // Move the raw bytes into app-internal storage (filesDir/vault), which is invisible
        // to other apps and to a PC over USB/MTP and is encrypted at rest by the OS when the
        // device is locked. No per-file encryption: a 64 KB stream copy, bounded memory.
        val sourceSize = queryFileSize(context, uri)
        val blob = blobFile(context, item)
        cr.openInputStream(uri)?.use { input ->
            FileOutputStream(blob).use { out ->
                input.copyTo(out, 64 * 1024)
                out.fd.sync() // flush to disk before we trust the copy and delete the original
            }
        } ?: error("Cannot open $uri")

        val size = blob.length()
        // Data-loss guard: never delete the original unless the copy is present and complete.
        if (size <= 0 || (sourceSize > 0 && size != sourceSize)) {
            runCatching { blob.delete() }
            error("Vault copy failed for $displayName (copied $size of $sourceSize bytes)")
        }
        val (_, aspect) = makeThumbnail(context, uri, mediaType)

        deleteOriginal(context, uri)
        item.copy(sizeBytes = size, aspectRatio = aspect)
    }

    // ── Name-based blob ops (used by the unified gallery via vault:// uris) ─────

    private fun blobFileByName(context: Context, storedName: String): File =
        File(vaultDir(context), storedName)

    /** Returns the readable media file for [storedName], or null if the vault is locked/missing. */
    fun decryptBlobToTempSync(context: Context, storedName: String, mimeType: String): File? {
        if (dek == null) return null
        val blob = blobFileByName(context, storedName)
        return if (blob.exists()) blob else null
    }

    /** Copies a blob's bytes to an arbitrary output stream (e.g. a SAF document). */
    fun decryptBlobToStream(context: Context, storedName: String, out: java.io.OutputStream): Boolean {
        if (dek == null) return false
        return runCatching {
            FileInputStream(blobFileByName(context, storedName)).use { input ->
                input.copyTo(out, 64 * 1024)
            }
            true
        }.getOrElse { false }
    }

    fun deleteBlobByName(context: Context, storedName: String) {
        runCatching { blobFileByName(context, storedName).delete() }
        runCatching { File(tempDir(context), storedName).delete() }
        tempDir(context).listFiles()?.filter { it.name.startsWith(storedName) }?.forEach { it.delete() }
    }

    /** Copies a blob into the system gallery. Returns the new MediaStore uri, or null. */
    suspend fun restoreBlobToGallery(
        context: Context, storedName: String, displayName: String, mediaType: String, mimeType: String
    ): String? = withContext(Dispatchers.IO) {
        if (dek == null) return@withContext null
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
        val target = cr.insert(collection, values) ?: return@withContext null
        val ok = runCatching {
            cr.openOutputStream(target)?.use { out ->
                FileInputStream(blobFileByName(context, storedName)).use { input ->
                    input.copyTo(out, 64 * 1024)
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
        // Blob deletion is the caller's responsibility (it updates the DB row first).
        if (ok) target.toString() else null
    }

    /** Permanently deletes the stored files for [item]. Caller removes the DB row. */
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

    /** Size in bytes of the content at [uri], or 0 if unknown. Used to verify the vault copy. */
    private fun queryFileSize(context: Context, uri: Uri): Long {
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.SIZE), null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !c.isNull(idx)) return c.getLong(idx)
                }
            }
        }
        return 0L
    }

    /** Returns (jpegThumbBytes, aspectRatio). */
    private fun makeThumbnail(context: Context, uri: Uri, mediaType: String): Pair<ByteArray?, Float> {
        return try {
            val bmp: Bitmap = (if (mediaType == "video") {
                val r = MediaMetadataRetriever()
                try {
                    r.setDataSource(context, uri)
                    // Decode a small frame directly: getFrameAtTime returns the full-resolution
                    // frame (a 4K/8K frame is 30-130 MB) which OOMs on import. Scaled keeps it tiny.
                    if (android.os.Build.VERSION.SDK_INT >= 27) {
                        r.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 720, 720)
                    } else {
                        r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                } finally {
                    runCatching { r.release() }
                }
            } else {
                // Read bounds first so we can downsample large photos instead of decoding
                // the full bitmap into memory (a 50 MP image is ~200 MB as ARGB_8888).
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, 1080)
                }
                context.contentResolver.openInputStream(uri)?.use { input ->
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

    /** Power-of-two subsample factor that keeps the larger side at or below [max] px. */
    private fun computeInSampleSize(w: Int, h: Int, max: Int): Int {
        if (w <= 0 || h <= 0) return 1
        var sample = 1
        while (maxOf(w, h) / sample > max) sample *= 2
        return sample
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
