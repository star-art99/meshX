package io.meshcore.crypto.storage

import io.meshcore.core.errors.StorageError
import io.meshcore.crypto.aead.ChaCha20Poly1305Aead
import io.meshcore.crypto.identity.Ed25519Identity
import io.meshcore.crypto.identity.fromHex
import io.meshcore.crypto.identity.toHex
import java.io.File
import java.nio.file.Path
import java.security.SecureRandom

/**
 * Secure local key storage interface.
 *
 * Private keys are stored encrypted with a derived key.
 * In production, this should be backed by OS keychain or a hardware security module.
 */
interface KeyStore {
    /** Save an identity's private key. The key is encrypted before writing. */
    fun saveIdentity(id: String, identity: Ed25519Identity)

    /** Load and decrypt an identity. Returns null if not found. */
    fun loadIdentity(id: String): Ed25519Identity?

    /** List all stored identity IDs. */
    fun listIdentities(): List<String>

    /** Delete a stored identity. */
    fun deleteIdentity(id: String): Boolean
}

/**
 * File-based encrypted key store.
 *
 * Each identity is stored as an encrypted file under [storageDir].
 * The key used for encryption is derived from the [passphrase] using HKDF.
 *
 * Security notes:
 * - Private keys are encrypted with ChaCha20-Poly1305 before writing to disk.
 * - The passphrase is used to derive the encryption key; it is NOT stored.
 * - TODO: In production, integrate with OS keychain (libsecret on Linux) or
 *   use a stronger KDF like Argon2id for passphrase-based key wrapping.
 */
class FileKeyStore(
    private val storageDir: Path,
    private val passphrase: ByteArray
) : KeyStore {

    init {
        storageDir.toFile().mkdirs()
    }

    private fun deriveFileKey(id: String): ByteArray {
        val salt = id.encodeToByteArray()
        return io.meshcore.crypto.aead.Hkdf.derive(
            inputKeyMaterial = passphrase,
            salt = salt,
            info = "meshcore-keystore-v1".encodeToByteArray()
        )
    }

    override fun saveIdentity(id: String, identity: Ed25519Identity) {
        val fileKey = deriveFileKey(id)
        val privateKeyBytes = identity.exportPrivateKeyBytes()
        val publicKeyBytes = identity.publicKey.bytes
        // Store: [32-byte pub][32-byte priv encrypted]
        val payload = publicKeyBytes + privateKeyBytes
        val encrypted = ChaCha20Poly1305Aead.encrypt(fileKey, payload)
        val file = storageDir.resolve("$id.key").toFile()
        file.writeBytes(encrypted)
    }

    override fun loadIdentity(id: String): Ed25519Identity? {
        val file = storageDir.resolve("$id.key").toFile()
        if (!file.exists()) return null
        return try {
            val fileKey = deriveFileKey(id)
            val decrypted = ChaCha20Poly1305Aead.decrypt(fileKey, file.readBytes())
            require(decrypted.size >= 64) { "Unexpected key data size" }
            val privateKeyBytes = decrypted.copyOfRange(32, 64)
            Ed25519Identity.fromPrivateKeyBytes(privateKeyBytes)
        } catch (e: Exception) {
            throw StorageError("Failed to load identity '$id'", e)
        }
    }

    override fun listIdentities(): List<String> {
        return storageDir.toFile()
            .listFiles { f -> f.name.endsWith(".key") }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    override fun deleteIdentity(id: String): Boolean {
        return storageDir.resolve("$id.key").toFile().delete()
    }
}
