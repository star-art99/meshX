package io.meshcore.storage.repository

import io.meshcore.storage.db.SqliteDatabase
import java.time.Instant

/**
 * Metadata record for a stored message.
 * Encrypted content is stored as a blob; decryption happens at the service layer.
 */
data class MessageRecord(
    val id: String,
    val fromPeerId: String,
    val toPeerId: String,
    val contentHash: String,
    val encryptedBlob: ByteArray,
    val sentAt: Instant,
    val deliveredAt: Instant?,
    val readAt: Instant?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MessageRecord) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

/**
 * Repository for message metadata and encrypted payloads.
 */
class MessageRepository(private val db: SqliteDatabase) {

    fun save(message: MessageRecord) {
        db.transaction { conn ->
            conn.prepareStatement(
                """INSERT OR REPLACE INTO messages
                   (id, from_peer_id, to_peer_id, content_hash, encrypted_blob, sent_at, delivered_at, read_at)
                   VALUES(?,?,?,?,?,?,?,?)"""
            ).use { ps ->
                ps.setString(1, message.id)
                ps.setString(2, message.fromPeerId)
                ps.setString(3, message.toPeerId)
                ps.setString(4, message.contentHash)
                ps.setBytes(5, message.encryptedBlob)
                ps.setString(6, message.sentAt.toString())
                ps.setString(7, message.deliveredAt?.toString())
                ps.setString(8, message.readAt?.toString())
                ps.execute()
            }
        }
    }

    fun findById(id: String): MessageRecord? {
        return db.connection.prepareStatement(
            "SELECT * FROM messages WHERE id=?"
        ).use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.toMessageRecord() else null
            }
        }
    }

    fun findByPeer(peerId: String, limit: Int = 50): List<MessageRecord> {
        return db.connection.prepareStatement(
            """SELECT * FROM messages
               WHERE from_peer_id=? OR to_peer_id=?
               ORDER BY sent_at DESC LIMIT ?"""
        ).use { ps ->
            ps.setString(1, peerId)
            ps.setString(2, peerId)
            ps.setInt(3, limit)
            ps.executeQuery().use { rs ->
                val list = mutableListOf<MessageRecord>()
                while (rs.next()) list.add(rs.toMessageRecord())
                list
            }
        }
    }

    fun markDelivered(id: String) {
        db.transaction { conn ->
            conn.prepareStatement(
                "UPDATE messages SET delivered_at=datetime('now') WHERE id=? AND delivered_at IS NULL"
            ).use { ps ->
                ps.setString(1, id)
                ps.execute()
            }
        }
    }

    fun markRead(id: String) {
        db.transaction { conn ->
            conn.prepareStatement(
                "UPDATE messages SET read_at=datetime('now') WHERE id=? AND read_at IS NULL"
            ).use { ps ->
                ps.setString(1, id)
                ps.execute()
            }
        }
    }

    fun delete(id: String): Boolean {
        return db.transaction { conn ->
            conn.prepareStatement("DELETE FROM messages WHERE id=?").use { ps ->
                ps.setString(1, id)
                ps.executeUpdate() > 0
            }
        }
    }

    private fun java.sql.ResultSet.toMessageRecord() = MessageRecord(
        id = getString("id"),
        fromPeerId = getString("from_peer_id"),
        toPeerId = getString("to_peer_id"),
        contentHash = getString("content_hash"),
        encryptedBlob = getBytes("encrypted_blob"),
        sentAt = parseInstant(getString("sent_at")),
        deliveredAt = getString("delivered_at")?.let { parseInstant(it) },
        readAt = getString("read_at")?.let { parseInstant(it) }
    )
}
