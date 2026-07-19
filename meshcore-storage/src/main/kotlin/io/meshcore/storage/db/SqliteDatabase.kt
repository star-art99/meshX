package io.meshcore.storage.db

import io.meshcore.core.errors.StorageError
import io.meshcore.core.logging.MeshLogger
import io.meshcore.storage.migration.MigrationRunner
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

private val log = MeshLogger.getLogger("io.meshcore.storage.db")

/**
 * SQLite database connection manager.
 *
 * Opens a connection to the local SQLite database file,
 * applies pending migrations, and provides access to the connection.
 *
 * Security note: SQLite's default mode is unencrypted.
 * TODO: Integrate SQLCipher for encrypted-at-rest storage
 *       (org.xerial:sqlite-jdbc supports loading native SQLCipher via custom path).
 */
class SqliteDatabase(
    private val dbPath: Path
) : AutoCloseable {

    private var _connection: Connection? = null

    val connection: Connection
        get() = _connection ?: throw StorageError("Database not initialized")

    /**
     * Open the database and run all pending migrations.
     */
    fun open() {
        try {
            dbPath.parent?.toFile()?.mkdirs()
            Class.forName("org.sqlite.JDBC")
            val url = "jdbc:sqlite:${dbPath.toAbsolutePath()}"
            val conn = DriverManager.getConnection(url)
            // Must be auto-commit for PRAGMA statements that can't run in a transaction
            conn.autoCommit = true

            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA journal_mode=WAL")
                stmt.execute("PRAGMA foreign_keys=ON")
                stmt.execute("PRAGMA synchronous=NORMAL")
            }

            // Switch to manual transaction control after pragmas
            conn.autoCommit = false
            _connection = conn
            log.info("Opened SQLite database at {}", dbPath)

            MigrationRunner(conn).runPendingMigrations()
        } catch (e: Exception) {
            throw StorageError("Failed to open database at $dbPath", e)
        }
    }

    /**
     * Execute a block within a database transaction.
     * Commits on success, rolls back on exception.
     */
    fun <T> transaction(block: (Connection) -> T): T {
        val conn = connection
        return try {
            val result = block(conn)
            conn.commit()
            result
        } catch (e: Exception) {
            conn.rollback()
            throw e
        }
    }

    override fun close() {
        _connection?.let {
            try {
                it.close()
                log.debug("SQLite database closed")
            } catch (e: Exception) {
                log.warn("Error closing database: {}", e.message)
            }
        }
        _connection = null
    }
}
