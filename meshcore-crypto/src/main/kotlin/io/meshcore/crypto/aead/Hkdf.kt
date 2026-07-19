package io.meshcore.crypto.aead

import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

/**
 * HKDF key derivation (RFC 5869) using SHA-256.
 *
 * Use this to derive per-session keys from the X25519 shared secret.
 *
 * Example:
 *   val sharedSecret = x25519KeyPair.agree(remotePubKey)
 *   val encKey = Hkdf.derive(sharedSecret, salt = nodeId.encodeToByteArray(), info = "enc".encodeToByteArray())
 */
object Hkdf {

    /**
     * Derive [outputLength] bytes from [inputKeyMaterial].
     *
     * @param inputKeyMaterial  the raw key material (e.g., X25519 shared secret)
     * @param salt              optional random salt
     * @param info              context / purpose string (should vary per use-case)
     * @param outputLength      desired output length in bytes (default: 32)
     */
    fun derive(
        inputKeyMaterial: ByteArray,
        salt: ByteArray? = null,
        info: ByteArray = ByteArray(0),
        outputLength: Int = 32
    ): ByteArray {
        val gen = HKDFBytesGenerator(SHA256Digest())
        val params = if (salt != null) {
            HKDFParameters(inputKeyMaterial, salt, info)
        } else {
            HKDFParameters.skipExtractParameters(inputKeyMaterial, info)
        }
        gen.init(params)
        val output = ByteArray(outputLength)
        gen.generateBytes(output, 0, outputLength)
        return output
    }
}
