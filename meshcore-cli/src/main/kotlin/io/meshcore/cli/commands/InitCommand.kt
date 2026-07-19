package io.meshcore.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.meshcore.core.config.MeshConfig
import io.meshcore.crypto.identity.Ed25519Identity
import io.meshcore.crypto.storage.FileKeyStore
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * Initialize a new MeshCore node identity.
 */
class InitCommand : CliktCommand(
    name = "init",
    help = "Initialize a new MeshCore node identity and configuration"
) {
    private val dataDir: Path by option(
        "--data-dir", "-d",
        help = "Data directory (default: ~/.meshcore)"
    ).path().default(MeshConfig.defaultDataDir())

    private val alias: String by option(
        "--alias", "-a",
        help = "Human-readable alias for this node"
    ).default("")

    override fun run() {
        dataDir.createDirectories()
        val configFile = dataDir.resolve("meshcore.properties").toFile()

        if (configFile.exists() && dataDir.resolve("keys").exists()) {
            echo("⚠ Node already initialized at $dataDir")
            echo("  Use --data-dir to specify a different location.")
            return
        }

        echo("Initializing MeshCore node...")

        // Generate identity
        val identity = Ed25519Identity.generate()
        val nodeId = if (alias.isNotBlank()) alias else "node-${identity.publicKey.toHex().take(8)}"

        // Save key
        val keysDir = dataDir.resolve("keys")
        keysDir.createDirectories()
        // Use empty passphrase for init (TODO: prompt for passphrase in interactive mode)
        val keyStore = FileKeyStore(keysDir, ByteArray(0))
        keyStore.saveIdentity("self", identity)

        // Save config
        val config = MeshConfig.load(null).copy(nodeId = nodeId, dataDir = dataDir)
        config.saveTo(configFile)

        echo("")
        echo("✓ Node initialized successfully!")
        echo("  Node ID  : $nodeId")
        echo("  Public key: ${identity.publicKey.toHex()}")
        echo("  Data dir : $dataDir")
        echo("  Config   : $configFile")
        echo("")
        echo("Run 'meshcore serve' to start the node.")

        identity.destroy()
    }
}
