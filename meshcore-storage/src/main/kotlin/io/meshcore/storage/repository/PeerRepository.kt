package io.meshcore.storage.repository

import io.meshcore.core.errors.StorageError
import io.meshcore.storage.db.SqliteDatabase
import java.time.Instant

/**
 * Represents a known peer in the mesh network.
 */
data class PeerRecord(
    val id: String,
    val publicKey: String,
    val alias: String?,
    val lastSeenAt: Instant?,
    val address: String?,
    val isTrusted: Boolean,
    val createdAt: Instant
)

/**
 * Repository for managing peer records in the local database.
 */
class PeerRepository(private val db: SqliteDatabase) {

    fun save(peer: PeerRecord) {
        db.transaction { conn ->
            conn.prepareStatement(
                """INSERT INTO peers(id, public_key, alias, last_seen_at, address, is_trusted, created_at)
                   VALUES(?,?,?,?,?,?,?)
                   ON CONFLICT(id) DO UPDATE SET
                       alias=excluded.alias,
                       last_seen_at=excluded.last_seen_at,
                       address=excluded.address,
                       is_trusted=excluded.is_trusted"""
            ).use { ps ->
                ps.setString(1, peer.id)
                ps.setString(2, peer.publicKey)
                ps.setString(3, peer.alias)
                ps.setString(4, peer.lastSeenAt?.toString())
                ps.setString(5, peer.address)
                ps.setInt(6, if (peer.isTrusted) 1 else 0)
                ps.setString(7, peer.createdAt.toString())
                ps.execute()
            }
        }
    }

    fun findById(id: String): PeerRecord? {
        return db.connection.prepareStatement(
            "SELECT * FROM peers WHERE id = ?"
        ).use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.toPeerRecord() else null
            }
        }
    }

    fun findByPublicKey(publicKey: String): PeerRecord? {
        return db.connection.prepareStatement(
            "SELECT * FROM peers WHERE public_key = ?"
        ).use { ps ->
            ps.setString(1, publicKey)
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.toPeerRecord() else null
            }
        }
    }

    fun findAll(): List<PeerRecord> {
        return db.connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT * FROM peers ORDER BY created_at DESC").use { rs ->
                val peers = mutableListOf<PeerRecord>()
                while (rs.next()) peers.add(rs.toPeerRecord())
                peers
            }
        }
    }

    fun updateLastSeen(id: String, address: String?) {
        db.transaction { conn ->
            conn.prepareStatement(
                "UPDATE peers SET last_seen_at=datetime('now'), address=? WHERE id=?"
            ).use { ps ->
                ps.setString(1, address)
                ps.setString(2, id)
                ps.execute()
            }
        }
    }

    fun delete(id: String): Boolean {
        return db.transaction { conn ->
            conn.prepareStatement("DELETE FROM peers WHERE id=?").use { ps ->
                ps.setString(1, id)
                ps.executeUpdate() > 0
            }
        }
    }

    private fun java.sql.ResultSet.toPeerRecord() = PeerRecord(
        id = getString("id"),
        publicKey = getString("public_key"),
        alias = getString("alias"),
        lastSeenAt = getString("last_seen_at")?.let { parseInstant(it) },
        address = getString("address"),
        isTrusted = getInt("is_trusted") == 1,
        createdAt = parseInstant(getString("created_at"))
    )
}
