package io.meshcore.crypto

import io.meshcore.core.errors.DecryptionError
import io.meshcore.core.errors.SignatureVerificationError
import io.meshcore.crypto.aead.ChaCha20Poly1305Aead
import io.meshcore.crypto.aead.Hkdf
import io.meshcore.crypto.identity.*
import io.meshcore.crypto.storage.FileKeyStore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.*
import java.nio.file.Files

class Ed25519IdentityTest {

    @Test
    fun `generate creates a valid identity`() {
        val identity = Ed25519Identity.generate()
        assertEquals(32, identity.publicKey.bytes.size)
        assertNotNull(identity.publicKey.toHex())
    }

    @Test
    fun `sign and verify roundtrip`() {
        val identity = Ed25519Identity.generate()
        val message = "hello, mesh!".encodeToByteArray()
        val signature = identity.sign(message)
        assertEquals(64, signature.size)
        assertTrue(verifyEd25519Signature(identity.publicKey, message, signature))
    }

    @Test
    fun `verify fails for tampered message`() {
        val identity = Ed25519Identity.generate()
        val message = "original".encodeToByteArray()
        val signature = identity.sign(message)
        val tampered = "tampered".encodeToByteArray()
        assertFalse(verifyEd25519Signature(identity.publicKey, tampered, signature))
    }

    @Test
    fun `requireValidSignature throws on invalid signature`() {
        val identity = Ed25519Identity.generate()
        val message = "test".encodeToByteArray()
        val badSig = ByteArray(64)
        assertThrows<SignatureVerificationError> {
            requireValidSignature(identity.publicKey, message, badSig)
        }
    }

    @Test
    fun `fromPrivateKeyBytes restores identity`() {
        val original = Ed25519Identity.generate()
        val privBytes = original.exportPrivateKeyBytes()
        val restored = Ed25519Identity.fromPrivateKeyBytes(privBytes)
        assertEquals(original.publicKey.toHex(), restored.publicKey.toHex())
    }

    @Test
    fun `publicKey hex encoding roundtrip`() {
        val identity = Ed25519Identity.generate()
        val hex = identity.publicKey.toHex()
        val restored = Ed25519PublicKey.fromHex(hex)
        assertContentEquals(identity.publicKey.bytes, restored.bytes)
    }
}

class X25519KeyAgreementTest {

    @Test
    fun `both parties derive the same shared secret`() {
        val alice = X25519KeyPair.generate()
        val bob = X25519KeyPair.generate()

        val aliceSecret = alice.agree(bob.publicKey)
        val bobSecret = bob.agree(alice.publicKey)

        assertContentEquals(aliceSecret, bobSecret)
        assertEquals(32, aliceSecret.size)
    }

    @Test
    fun `different key pairs produce different secrets`() {
        val alice = X25519KeyPair.generate()
        val bob = X25519KeyPair.generate()
        val carol = X25519KeyPair.generate()

        val aliceBob = alice.agree(bob.publicKey)
        val aliceCarol = alice.agree(carol.publicKey)

        assertFalse(aliceBob.contentEquals(aliceCarol))
    }
}

class ChaCha20Poly1305AeadTest {

    @Test
    fun `encrypt and decrypt roundtrip`() {
        val key = ChaCha20Poly1305Aead.generateKey()
        val plaintext = "Hello, MeshCore!".encodeToByteArray()
        val ciphertext = ChaCha20Poly1305Aead.encrypt(key, plaintext)
        val decrypted = ChaCha20Poly1305Aead.decrypt(key, ciphertext)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun `ciphertext is longer than plaintext by nonce and tag`() {
        val key = ChaCha20Poly1305Aead.generateKey()
        val plaintext = "test".encodeToByteArray()
        val ciphertext = ChaCha20Poly1305Aead.encrypt(key, plaintext)
        // nonce(12) + tag(16) + plaintext
        assertEquals(plaintext.size + ChaCha20Poly1305Aead.NONCE_SIZE + ChaCha20Poly1305Aead.TAG_SIZE, ciphertext.size)
    }

    @Test
    fun `different encryptions of same plaintext produce different ciphertexts`() {
        val key = ChaCha20Poly1305Aead.generateKey()
        val plaintext = "same message".encodeToByteArray()
        val ct1 = ChaCha20Poly1305Aead.encrypt(key, plaintext)
        val ct2 = ChaCha20Poly1305Aead.encrypt(key, plaintext)
        // Different nonces → different ciphertexts
        assertFalse(ct1.contentEquals(ct2))
    }

    @Test
    fun `decrypt with wrong key throws DecryptionError`() {
        val key = ChaCha20Poly1305Aead.generateKey()
        val wrongKey = ChaCha20Poly1305Aead.generateKey()
        val ciphertext = ChaCha20Poly1305Aead.encrypt(key, "secret".encodeToByteArray())
        assertThrows<DecryptionError> {
            ChaCha20Poly1305Aead.decrypt(wrongKey, ciphertext)
        }
    }

    @Test
    fun `encrypt with AAD, decrypt without AAD throws`() {
        val key = ChaCha20Poly1305Aead.generateKey()
        val aad = "metadata".encodeToByteArray()
        val ct = ChaCha20Poly1305Aead.encrypt(key, "payload".encodeToByteArray(), aad)
        assertThrows<DecryptionError> {
            ChaCha20Poly1305Aead.decrypt(key, ct, null) // AAD mismatch
        }
    }
}

class HkdfTest {

    @Test
    fun `derive produces expected length output`() {
        val ikm = ByteArray(32) { it.toByte() }
        val out = Hkdf.derive(ikm, outputLength = 32)
        assertEquals(32, out.size)
    }

    @Test
    fun `same inputs produce same output`() {
        val ikm = "shared-secret".encodeToByteArray()
        val salt = "salt".encodeToByteArray()
        val info = "enc".encodeToByteArray()
        val a = Hkdf.derive(ikm, salt, info)
        val b = Hkdf.derive(ikm, salt, info)
        assertContentEquals(a, b)
    }

    @Test
    fun `different info produces different output`() {
        val ikm = "shared-secret".encodeToByteArray()
        val a = Hkdf.derive(ikm, info = "enc".encodeToByteArray())
        val b = Hkdf.derive(ikm, info = "mac".encodeToByteArray())
        assertFalse(a.contentEquals(b))
    }
}

class FileKeyStoreTest {

    @Test
    fun `save and load identity roundtrip`() {
        val tmpDir = Files.createTempDirectory("meshcore-keystore-test")
        val keyStore = FileKeyStore(tmpDir, "testpass".encodeToByteArray())

        val identity = Ed25519Identity.generate()
        keyStore.saveIdentity("alice", identity)

        val loaded = keyStore.loadIdentity("alice")
        assertNotNull(loaded)
        assertEquals(identity.publicKey.toHex(), loaded.publicKey.toHex())
    }

    @Test
    fun `load non-existent identity returns null`() {
        val tmpDir = Files.createTempDirectory("meshcore-keystore-test2")
        val keyStore = FileKeyStore(tmpDir, ByteArray(0))
        assertNull(keyStore.loadIdentity("nonexistent"))
    }

    @Test
    fun `listIdentities returns saved ids`() {
        val tmpDir = Files.createTempDirectory("meshcore-keystore-test3")
        val keyStore = FileKeyStore(tmpDir, ByteArray(0))
        keyStore.saveIdentity("id1", Ed25519Identity.generate())
        keyStore.saveIdentity("id2", Ed25519Identity.generate())
        val ids = keyStore.listIdentities()
        assertTrue("id1" in ids)
        assertTrue("id2" in ids)
    }

    @Test
    fun `deleteIdentity removes key file`() {
        val tmpDir = Files.createTempDirectory("meshcore-keystore-test4")
        val keyStore = FileKeyStore(tmpDir, ByteArray(0))
        keyStore.saveIdentity("to-delete", Ed25519Identity.generate())
        assertTrue(keyStore.deleteIdentity("to-delete"))
        assertNull(keyStore.loadIdentity("to-delete"))
    }
}
