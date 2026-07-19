package io.meshcore.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import io.meshcore.api.server.ApiServer
import io.meshcore.core.config.MeshConfig
import io.meshcore.core.events.EventBus
import io.meshcore.core.lifecycle.NodeLifecycle
import io.meshcore.storage.db.SqliteDatabase
import io.meshcore.storage.repository.PeerRepository
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Start the MeshCore node (API server + discovery).
 */
class ServeCommand : CliktCommand(
    name = "serve",
    help = "Start the MeshCore node (API server + peer discovery)"
) {
    private val dataDir: Path by option(
        "--data-dir", "-d",
        help = "Data directory (default: ~/.meshcore)"
    ).path().default(MeshConfig.defaultDataDir())

    private val port: Int by option(
        "--port", "-p",
        help = "API server port (default: 7470)"
    ).int().default(7470)

    override fun run() {
        dataDir.createDirectories()
        val configFile = dataDir.resolve("meshcore.properties").toFile()
        val config = MeshConfig.load(if (configFile.exists()) configFile else null).copy(dataDir = dataDir)

        echo("Starting MeshCore node [id=${config.nodeId}]")
        echo("API: http://${config.apiHost}:${config.apiPort}")
        echo("Press Ctrl+C to stop.")
        echo("")

        runBlocking {
            // Open database
            val dbPath = dataDir.resolve("meshcore.db")
            val database = SqliteDatabase(dbPath)
            database.open()

            val peerRepository = PeerRepository(database)
            val eventBus = EventBus()
            val lifecycle = NodeLifecycle(config, eventBus)

            val apiServer = ApiServer(config, peerRepository)
            lifecycle.register(apiServer)

            lifecycle.registerShutdownHook {
                echo("Shutting down...")
                lifecycle.stop()
                database.close()
            }

            lifecycle.start()

            // Keep running until interrupted
            try {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // normal shutdown
            } finally {
                lifecycle.stop()
                database.close()
            }
        }
    }
}
