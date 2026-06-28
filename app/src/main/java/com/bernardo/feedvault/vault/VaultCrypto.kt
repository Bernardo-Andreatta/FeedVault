package com.bernardo.feedvault.vault

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Crypto primitives for the safe folder.
 *
 * Design (envelope encryption):
 *  - A random 256-bit AES Data Encryption Key (DEK) encrypts every media file.
 *  - The DEK is itself wrapped by a password-derived key (PBKDF2) and,
 *    optionally, by a biometric-gated Android Keystore key. Either wrapper can
 *    unwrap the DEK; the password is always the recovery path.
 *  - Files are encrypted with AES-GCM streaming over the raw bytes, so a
 *    decrypted file is byte-identical to the original — no re-encoding, no
 *    quality loss, works for photos, videos and GIFs alike.
 */
object VaultCrypto {

    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val GCM_IV_BYTES = 12
    private const val KEYSTORE = "AndroidKeyStore"
    const val BIO_KEY_ALIAS = "feedvault_vault_bio_key"

    private val rng = SecureRandom()

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { rng.nextBytes(it) }

    fun generateDek(): SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(KEY_BITS)
        return kg.generateKey()
    }

    fun secretKeyFromBytes(bytes: ByteArray): SecretKey = SecretKeySpec(bytes, "AES")

    /** Derive an AES key from the user's password and a stored salt. */
    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    // ── Small-blob wrap/unwrap (used to protect the DEK) ──────────────────────

    /** Returns iv || ciphertext. */
    fun aesGcmEncrypt(key: SecretKey, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val ct = cipher.doFinal(plaintext)
        return iv + ct
    }

    /** Input is iv || ciphertext. Throws on wrong key / tampering. */
    fun aesGcmDecrypt(key: SecretKey, ivAndCt: ByteArray): ByteArray {
        val iv = ivAndCt.copyOfRange(0, GCM_IV_BYTES)
        val ct = ivAndCt.copyOfRange(GCM_IV_BYTES, ivAndCt.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    // ── File streaming (used for the media bytes themselves) ──────────────────

    /** Encrypts [input] into [output]. Layout on disk: iv (12 bytes) || GCM ciphertext. */
    fun encryptStream(dek: SecretKey, input: InputStream, output: OutputStream) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, dek)
        output.write(cipher.iv)
        CipherOutputStream(output, cipher).use { cos ->
            input.copyTo(cos, 64 * 1024)
        }
    }

    /** Decrypts an [input] produced by [encryptStream] into [output]. */
    fun decryptStream(dek: SecretKey, input: InputStream, output: OutputStream) {
        val iv = ByteArray(GCM_IV_BYTES)
        var read = 0
        while (read < GCM_IV_BYTES) {
            val r = input.read(iv, read, GCM_IV_BYTES - read)
            if (r < 0) throw IllegalStateException("Truncated vault file")
            read += r
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, dek, GCMParameterSpec(GCM_TAG_BITS, iv))
        CipherInputStream(input, cipher).use { cis ->
            cis.copyTo(output, 64 * 1024)
        }
    }

    // ── Biometric-gated keystore key ──────────────────────────────────────────

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    fun biometricKeyExists(): Boolean = keyStore().containsAlias(BIO_KEY_ALIAS)

    fun deleteBiometricKey() {
        runCatching { keyStore().deleteEntry(BIO_KEY_ALIAS) }
    }

    /** Creates (or replaces) the keystore key that requires biometric auth on every use. */
    private fun createBiometricKey(): SecretKey {
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            BIO_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_BITS)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        kg.init(spec)
        return kg.generateKey()
    }

    private fun biometricKey(): SecretKey {
        val ks = keyStore()
        return (ks.getEntry(BIO_KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /** Cipher to authorise via BiometricPrompt before wrapping the DEK (enabling biometric unlock). */
    fun biometricEncryptCipher(): Cipher {
        val key = createBiometricKey()
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    /** Cipher to authorise via BiometricPrompt before unwrapping the DEK (unlocking). */
    fun biometricDecryptCipher(iv: ByteArray): Cipher {
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, biometricKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        }
    }
}
