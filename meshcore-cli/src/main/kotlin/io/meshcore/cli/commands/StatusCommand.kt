package io.meshcore.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.meshcore.core.config.MeshConfig
import io.meshcore.core.lifecycle.NodeState
import io.meshcore.crypto.storage.FileKeyStore
import java.io.File
import java.net.URL
import java.nio.file.Path

/**
 * Show the current node status.
 */
class StatusCommand : CliktCommand(
    name = "status",
    help = "Show current node status"
) {
    private val dataDir: Path by option(
        "--data-dir", "-d",
        help = "Data directory (default: ~/.meshcore)"
    ).path().default(MeshConfig.defaultDataDir())

    override fun run() {
        val configFile = dataDir.resolve("meshcore.properties").toFile()
        val config = MeshConfig.load(if (configFile.exists()) configFile else null).copy(dataDir = dataDir)

        echo("MeshCore Node Status")
        echo("═══════════════════")
        echo("  Node ID  : ${config.nodeId}")
        echo("  Data dir : ${config.dataDir}")
        echo("  API      : http://${config.apiHost}:${config.apiPort}")
        echo("  Log level: ${config.logLevel}")
        echo("  Discovery: ${if (config.discoveryEnabled) "enabled" else "disabled"}")

        // Check if API is responding
        val apiStatus = try {
            val url = URL("http://${config.apiHost}:${config.apiPort}/health")
            val conn = url.openConnection()
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            conn.connect()
            "● running"
        } catch (e: Exception) {
            "○ not running"
        }
        echo("  API status: $apiStatus")

        // Show stored identities
        val keysDir = dataDir.resolve("keys")
        if (keysDir.toFile().exists()) {
            val keyStore = FileKeyStore(keysDir, ByteArray(0))
            val identities = keyStore.listIdentities()
            echo("  Identities: ${identities.size} stored")
        } else {
            echo("  Identities: none (run 'meshcore init' first)")
        }
    }
}
