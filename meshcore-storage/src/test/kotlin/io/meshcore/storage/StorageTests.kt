package io.meshcore.storage

import io.meshcore.storage.db.SqliteDatabase
import io.meshcore.storage.repository.MessageRecord
import io.meshcore.storage.repository.MessageRepository
import io.meshcore.storage.repository.PeerRecord
import io.meshcore.storage.repository.PeerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Instant
import kotlin.test.*

class StorageTests {

    private lateinit var db: SqliteDatabase
    private lateinit var peerRepo: PeerRepository
    private lateinit var messageRepo: MessageRepository

    @BeforeEach
    fun setUp() {
        val tmpDir = Files.createTempDirectory("meshcore-storage-test")
        db = SqliteDatabase(tmpDir.resolve("test.db"))
        db.open()
        peerRepo = PeerRepository(db)
        messageRepo = MessageRepository(db)
    }

    @AfterEach
    fun tearDown() {
        db.close()
    }

    @Test
    fun `save and findById peer`() {
        val peer = PeerRecord(
            id = "peer1",
            publicKey = "aabbccdd",
            alias = "Alice",
            lastSeenAt = null,
            address = "192.168.1.10:7480",
            isTrusted = true,
            createdAt = Instant.now()
        )
        peerRepo.save(peer)
        val found = peerRepo.findById("peer1")
        assertNotNull(found)
        assertEquals("peer1", found.id)
        assertEquals("Alice", found.alias)
        assertTrue(found.isTrusted)
    }

    @Test
    fun `findById returns null for missing peer`() {
        assertNull(peerRepo.findById("nonexistent"))
    }

    @Test
    fun `findAll returns saved peers`() {
        val now = Instant.now()
        peerRepo.save(PeerRecord("p1", "pk1", null, null, null, false, now))
        peerRepo.save(PeerRecord("p2", "pk2", null, null, null, false, now))
        val all = peerRepo.findAll()
        assertEquals(2, all.size)
    }

    @Test
    fun `save updates existing peer on conflict`() {
        val now = Instant.now()
        peerRepo.save(PeerRecord("p1", "pk1", "Alice", null, null, false, now))
        peerRepo.save(PeerRecord("p1", "pk1", "Alice Updated", null, "10.0.0.1:7480", true, now))
        val found = peerRepo.findById("p1")
        assertNotNull(found)
        assertEquals("Alice Updated", found.alias)
        assertTrue(found.isTrusted)
    }

    @Test
    fun `delete removes peer`() {
        val now = Instant.now()
        peerRepo.save(PeerRecord("p1", "pk1", null, null, null, false, now))
        assertTrue(peerRepo.delete("p1"))
        assertNull(peerRepo.findById("p1"))
    }

    @Test
    fun `save and findById message`() {
        val now = Instant.now()
        peerRepo.save(PeerRecord("from", "pk_from", null, null, null, false, now))
        peerRepo.save(PeerRecord("to", "pk_to", null, null, null, false, now))

        val msg = MessageRecord(
            id = "msg1",
            fromPeerId = "from",
            toPeerId = "to",
            contentHash = "abc123",
            encryptedBlob = "encrypted payload".encodeToByteArray(),
            sentAt = now,
            deliveredAt = null,
            readAt = null
        )
        messageRepo.save(msg)
        val found = messageRepo.findById("msg1")
        assertNotNull(found)
        assertEquals("msg1", found.id)
        assertEquals("abc123", found.contentHash)
        assertContentEquals("encrypted payload".encodeToByteArray(), found.encryptedBlob)
    }

    @Test
    fun `findByPeer returns messages for given peer`() {
        val now = Instant.now()
        peerRepo.save(PeerRecord("p1", "pk1", null, null, null, false, now))
        peerRepo.save(PeerRecord("p2", "pk2", null, null, null, false, now))

        messageRepo.save(MessageRecord("m1", "p1", "p2", "h1", ByteArray(1), now, null, null))
        messageRepo.save(MessageRecord("m2", "p2", "p1", "h2", ByteArray(1), now, null, null))

        val msgs = messageRepo.findByPeer("p1")
        assertEquals(2, msgs.size)
    }

    @Test
    fun `markDelivered sets delivered_at`() {
        val now = Instant.now()
        peerRepo.save(PeerRecord("pa", "pka", null, null, null, false, now))
        peerRepo.save(PeerRecord("pb", "pkb", null, null, null, false, now))
        messageRepo.save(MessageRecord("mx", "pa", "pb", "hx", ByteArray(0), now, null, null))
        messageRepo.markDelivered("mx")
        val found = messageRepo.findById("mx")
        assertNotNull(found?.deliveredAt)
    }
}
