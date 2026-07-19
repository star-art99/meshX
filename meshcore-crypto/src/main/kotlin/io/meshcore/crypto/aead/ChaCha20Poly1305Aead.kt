package io.meshcore.crypto.aead

import io.meshcore.core.errors.DecryptionError
import io.meshcore.core.errors.EncryptionError
import org.bouncycastle.crypto.engines.ChaCha7539Engine
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.modes.ChaCha20Poly1305
import org.bouncycastle.crypto.params.AEADParameters
import org.bouncycastle.crypto.params.KeyParameter
import java.security.SecureRandom

/**
 * AEAD encryption using ChaCha20-Poly1305 (RFC 7539 / IETF variant).
 *
 * Key size: 32 bytes (256-bit)
 * Nonce size: 12 bytes (96-bit, randomly generated per message)
 * Tag size: 16 bytes (128-bit)
 *
 * Output format: [12-byte nonce][ciphertext+tag]
 *
 * Security notes:
 * - A cryptographically secure random nonce is generated for every encryption.
 * - AAD (additional authenticated data) is optional but recommended for
 *   including metadata that should be authenticated but not encrypted.
 * - Do not reuse keys across different contexts – derive per-session keys
 *   from the X25519 shared secret using HKDF.
 */
object ChaCha20Poly1305Aead {

    const val KEY_SIZE = 32
    const val NONCE_SIZE = 12
    const val TAG_SIZE = 16

    private val secureRandom = SecureRandom()

    /**
     * Encrypt [plaintext] with [key].
     *
     * @param key     32-byte encryption key
     * @param plaintext  data to encrypt
     * @param aad     optional additional authenticated data
     * @return [nonce (12 bytes)] + [ciphertext + auth tag]
     */
    fun encrypt(key: ByteArray, plaintext: ByteArray, aad: ByteArray? = null): ByteArray {
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }
        return try {
            val nonce = generateNonce()
            val cipher = ChaCha20Poly1305()
            val params = AEADParameters(KeyParameter(key), TAG_SIZE * 8, nonce, aad)
            cipher.init(true, params)
            val ciphertext = ByteArray(cipher.getOutputSize(plaintext.size))
            val len = cipher.processBytes(plaintext, 0, plaintext.size, ciphertext, 0)
            cipher.doFinal(ciphertext, len)
            nonce + ciphertext
        } catch (e: Exception) {
            throw EncryptionError("ChaCha20-Poly1305 encryption failed", e)
        }
    }

    /**
     * Decrypt [ciphertextWithNonce] with [key].
     *
     * @param key                 32-byte decryption key
     * @param ciphertextWithNonce [nonce (12 bytes)] + [ciphertext + auth tag]
     * @param aad                 optional additional authenticated data (must match encryption)
     * @return plaintext bytes
     * @throws DecryptionError if authentication fails or decryption fails
     */
    fun decrypt(key: ByteArray, ciphertextWithNonce: ByteArray, aad: ByteArray? = null): ByteArray {
        require(key.size == KEY_SIZE) { "Key must be $KEY_SIZE bytes" }
        require(ciphertextWithNonce.size > NONCE_SIZE + TAG_SIZE) {
            "Ciphertext too short"
        }
        return try {
            val nonce = ciphertextWithNonce.copyOfRange(0, NONCE_SIZE)
            val ciphertext = ciphertextWithNonce.copyOfRange(NONCE_SIZE, ciphertextWithNonce.size)
            val cipher = ChaCha20Poly1305()
            val params = AEADParameters(KeyParameter(key), TAG_SIZE * 8, nonce, aad)
            cipher.init(false, params)
            val plaintext = ByteArray(cipher.getOutputSize(ciphertext.size))
            val len = cipher.processBytes(ciphertext, 0, ciphertext.size, plaintext, 0)
            cipher.doFinal(plaintext, len)
            plaintext
        } catch (e: Exception) {
            throw DecryptionError("ChaCha20-Poly1305 decryption or authentication failed", e)
        }
    }

    /** Generate a random 12-byte nonce. */
    fun generateNonce(): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonce)
        return nonce
    }

    /** Generate a random 32-byte key. */
    fun generateKey(): ByteArray {
        val key = ByteArray(KEY_SIZE)
        secureRandom.nextBytes(key)
        return key
    }
}
