package io.meshcore.crypto.identity

import io.meshcore.core.errors.KeyGenerationError
import io.meshcore.core.errors.SignatureVerificationError
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.security.SecureRandom

/**
 * Ed25519 public key – safe to share and serialize.
 */
@JvmInline
value class Ed25519PublicKey(val bytes: ByteArray) {
    fun toHex(): String = bytes.toHex()

    companion object {
        fun fromHex(hex: String): Ed25519PublicKey = Ed25519PublicKey(hex.fromHex())
        fun fromBytes(bytes: ByteArray): Ed25519PublicKey {
            require(bytes.size == 32) { "Ed25519 public key must be 32 bytes" }
            return Ed25519PublicKey(bytes.copyOf())
        }
    }
}

/**
 * Ed25519 private key – NEVER log, serialize to network, or expose via API.
 * Backed by a defensive copy; zeroed on [destroy].
 */
class Ed25519PrivateKey private constructor(private val keyBytes: ByteArray) {
    val bytes: ByteArray get() = keyBytes.copyOf()

    fun toHex(): String = keyBytes.toHex()

    /** Overwrite key material in memory (best-effort). */
    fun destroy() {
        keyBytes.fill(0)
    }

    companion object {
        fun fromBytes(bytes: ByteArray): Ed25519PrivateKey {
            require(bytes.size == 32 || bytes.size == 64) {
                "Ed25519 private key must be 32 or 64 bytes"
            }
            return Ed25519PrivateKey(bytes.copyOf(32))
        }

        fun fromHex(hex: String): Ed25519PrivateKey = fromBytes(hex.fromHex())

        internal fun fromRaw(bytes: ByteArray) = Ed25519PrivateKey(bytes.copyOf())
    }
}

/**
 * A complete Ed25519 identity (key pair) with sign/verify operations.
 */
class Ed25519Identity(
    val publicKey: Ed25519PublicKey,
    private val privateKey: Ed25519PrivateKey
) {
    /**
     * Sign [message] with this identity's private key.
     * Returns a 64-byte signature.
     */
    fun sign(message: ByteArray): ByteArray {
        val signer = Ed25519Signer()
        val privParams = Ed25519PrivateKeyParameters(privateKey.bytes)
        signer.init(true, privParams)
        signer.update(message, 0, message.size)
        return signer.generateSignature()
    }

    /**
     * Export the private key bytes for secure storage.
     * Caller is responsible for encryption before persisting.
     */
    fun exportPrivateKeyBytes(): ByteArray = privateKey.bytes

    /** Destroy private key material. */
    fun destroy() = privateKey.destroy()

    companion object {
        /**
         * Generate a fresh Ed25519 identity using a secure random source.
         */
        fun generate(): Ed25519Identity {
            return try {
                val rng = SecureRandom()
                val generator = Ed25519KeyPairGenerator()
                generator.init(Ed25519KeyGenerationParameters(rng))
                val keyPair = generator.generateKeyPair()
                val pub = (keyPair.public as Ed25519PublicKeyParameters).encoded
                val priv = (keyPair.private as Ed25519PrivateKeyParameters).encoded
                Ed25519Identity(
                    publicKey = Ed25519PublicKey(pub),
                    privateKey = Ed25519PrivateKey.fromRaw(priv)
                )
            } catch (e: Exception) {
                throw KeyGenerationError("Ed25519 key generation failed", e)
            }
        }

        /**
         * Restore an identity from raw key bytes (e.g., loaded from encrypted storage).
         */
        fun fromPrivateKeyBytes(privateKeyBytes: ByteArray): Ed25519Identity {
            val privParams = Ed25519PrivateKeyParameters(privateKeyBytes.copyOf(32))
            val pubParams = privParams.generatePublicKey()
            return Ed25519Identity(
                publicKey = Ed25519PublicKey(pubParams.encoded),
                privateKey = Ed25519PrivateKey.fromRaw(privateKeyBytes.copyOf(32))
            )
        }
    }
}

/**
 * Verify an Ed25519 signature.
 *
 * @throws SignatureVerificationError if the signature is invalid.
 */
fun verifyEd25519Signature(
    publicKey: Ed25519PublicKey,
    message: ByteArray,
    signature: ByteArray
): Boolean {
    val verifier = Ed25519Signer()
    val pubParams = Ed25519PublicKeyParameters(publicKey.bytes)
    verifier.init(false, pubParams)
    verifier.update(message, 0, message.size)
    return verifier.verifySignature(signature)
}

/**
 * Verify and throw on failure.
 */
fun requireValidSignature(
    publicKey: Ed25519PublicKey,
    message: ByteArray,
    signature: ByteArray
) {
    if (!verifyEd25519Signature(publicKey, message, signature)) {
        throw SignatureVerificationError()
    }
}

// --- Hex helpers (internal) ---

internal fun ByteArray.toHex(): String =
    joinToString("") { "%02x".format(it) }

internal fun String.fromHex(): ByteArray {
    check(length % 2 == 0) { "Hex string must have even length" }
    return ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}
