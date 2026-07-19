package io.meshcore.storage.migration

import io.meshcore.core.errors.MigrationError
import io.meshcore.core.logging.MeshLogger
import java.sql.Connection

private val log = MeshLogger.getLogger("io.meshcore.storage.migration")

/**
 * A single database migration step.
 */
data class Migration(
    val version: Int,
    val description: String,
    val sql: String
)

/**
 * Database migration runner.
 *
 * Maintains a schema_version table and applies pending migrations in order.
 * Migrations are applied within transactions for atomicity.
 */
class MigrationRunner(private val connection: Connection) {

    companion object {
        private val MIGRATIONS: List<Migration> = listOf(
            Migration(
                version = 1,
                description = "Initial schema – identities, peers, messages",
                sql = """
                    CREATE TABLE IF NOT EXISTS schema_version (
                        version     INTEGER NOT NULL PRIMARY KEY,
                        applied_at  TEXT    NOT NULL DEFAULT (datetime('now'))
                    );

                    CREATE TABLE IF NOT EXISTS identities (
                        id          TEXT    NOT NULL PRIMARY KEY,
                        public_key  TEXT    NOT NULL UNIQUE,
                        alias       TEXT,
                        created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
                        is_self     INTEGER NOT NULL DEFAULT 0
                    );

                    CREATE TABLE IF NOT EXISTS peers (
                        id              TEXT    NOT NULL PRIMARY KEY,
                        public_key      TEXT    NOT NULL UNIQUE,
                        alias           TEXT,
                        last_seen_at    TEXT,
                        address         TEXT,
                        is_trusted      INTEGER NOT NULL DEFAULT 0,
                        created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
                    );

                    CREATE TABLE IF NOT EXISTS messages (
                        id              TEXT    NOT NULL PRIMARY KEY,
                        from_peer_id    TEXT    NOT NULL,
                        to_peer_id      TEXT    NOT NULL,
                        content_hash    TEXT    NOT NULL,
                        encrypted_blob  BLOB    NOT NULL,
                        sent_at         TEXT    NOT NULL DEFAULT (datetime('now')),
                        delivered_at    TEXT,
                        read_at         TEXT,
                        FOREIGN KEY (from_peer_id) REFERENCES peers(id),
                        FOREIGN KEY (to_peer_id)   REFERENCES peers(id)
                    );

                    CREATE INDEX IF NOT EXISTS idx_messages_from_peer ON messages(from_peer_id);
                    CREATE INDEX IF NOT EXISTS idx_messages_to_peer   ON messages(to_peer_id);
                    CREATE INDEX IF NOT EXISTS idx_peers_public_key   ON peers(public_key);
                """.trimIndent()
            )
        )
    }

    fun runPendingMigrations() {
        ensureSchemaVersionTable()
        val applied = getAppliedVersions()
        val pending = MIGRATIONS.filter { it.version !in applied }

        if (pending.isEmpty()) {
            log.debug("No pending migrations")
            return
        }

        for (migration in pending.sortedBy { it.version }) {
            applyMigration(migration)
        }

        log.info("Applied {} migration(s)", pending.size)
    }

    private fun ensureSchemaVersionTable() {
        connection.createStatement().use { stmt ->
            stmt.execute(
                """CREATE TABLE IF NOT EXISTS schema_version (
                    version     INTEGER NOT NULL PRIMARY KEY,
                    applied_at  TEXT    NOT NULL DEFAULT (datetime('now'))
                )"""
            )
        }
        connection.commit()
    }

    private fun getAppliedVersions(): Set<Int> {
        val versions = mutableSetOf<Int>()
        connection.createStatement().use { stmt ->
            stmt.executeQuery("SELECT version FROM schema_version").use { rs ->
                while (rs.next()) versions.add(rs.getInt("version"))
            }
        }
        return versions
    }

    private fun applyMigration(migration: Migration) {
        log.info("Applying migration v{}: {}", migration.version, migration.description)
        try {
            // Execute each statement separately (SQLite JDBC doesn't support multi-statement execute)
            migration.sql.split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .forEach { sql ->
                    connection.createStatement().use { stmt -> stmt.execute(sql) }
                }
            connection.prepareStatement(
                "INSERT INTO schema_version(version) VALUES (?)"
            ).use { ps ->
                ps.setInt(1, migration.version)
                ps.execute()
            }
            connection.commit()
            log.info("Migration v{} applied successfully", migration.version)
        } catch (e: Exception) {
            connection.rollback()
            throw MigrationError(migration.version, e)
        }
    }
}
