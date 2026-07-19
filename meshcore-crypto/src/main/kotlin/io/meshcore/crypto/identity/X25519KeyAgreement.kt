package io.meshcore.crypto.identity

import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom

/**
 * X25519 key pair for Diffie-Hellman key agreement.
 *
 * Used for ephemeral session key establishment.
 * Private key material is never logged or exposed via API.
 */
class X25519KeyPair private constructor(
    val publicKey: ByteArray,
    private val privateKeyParams: X25519PrivateKeyParameters
) {
    companion object {
        fun generate(): X25519KeyPair {
            val gen = X25519KeyPairGenerator()
            gen.init(X25519KeyGenerationParameters(SecureRandom()))
            val pair = gen.generateKeyPair()
            val pub = (pair.public as X25519PublicKeyParameters).encoded
            val priv = pair.private as X25519PrivateKeyParameters
            return X25519KeyPair(pub, priv)
        }

        fun fromPrivateKeyBytes(bytes: ByteArray): X25519KeyPair {
            val priv = X25519PrivateKeyParameters(bytes)
            val pub = priv.generatePublicKey().encoded
            return X25519KeyPair(pub, priv)
        }
    }

    /**
     * Perform ECDH with the remote party's public key.
     * Returns a 32-byte shared secret.
     *
     * The shared secret should be passed through HKDF before use.
     */
    fun agree(remotePublicKey: ByteArray): ByteArray {
        val agreement = X25519Agreement()
        agreement.init(privateKeyParams)
        val remoteParams = X25519PublicKeyParameters(remotePublicKey)
        val sharedSecret = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(remoteParams, sharedSecret, 0)
        return sharedSecret
    }
}
