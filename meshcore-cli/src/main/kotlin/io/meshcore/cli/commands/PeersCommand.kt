package io.meshcore.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.meshcore.core.config.MeshConfig
import java.net.URL
import java.nio.file.Path

/**
 * List known peers.
 */
class PeersCommand : CliktCommand(
    name = "peers",
    help = "List known peers"
) {
    private val dataDir: Path by option(
        "--data-dir", "-d",
        help = "Data directory (default: ~/.meshcore)"
    ).path().default(MeshConfig.defaultDataDir())

    override fun run() {
        val configFile = dataDir.resolve("meshcore.properties").toFile()
        val config = MeshConfig.load(if (configFile.exists()) configFile else null).copy(dataDir = dataDir)

        try {
            val url = URL("http://${config.apiHost}:${config.apiPort}/api/v1/peers")
            val response = url.readText()
            echo("Peers:")
            echo(response)
        } catch (e: Exception) {
            echo("⚠ Could not connect to API at http://${config.apiHost}:${config.apiPort}")
            echo("  Is the node running? Try: meshcore serve")
        }
    }
}
