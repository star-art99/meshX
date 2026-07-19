package io.meshcore.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import io.meshcore.core.config.MeshConfig
import java.net.URL
import java.nio.file.Path

/**
 * Send a test message to a peer.
 */
class SendCommand : CliktCommand(
    name = "send",
    help = "Send a test message to a peer (requires running node)"
) {
    private val toPeerId: String by argument(help = "Target peer ID")
    private val message: String by argument(help = "Message content")
    private val dataDir: Path by option(
        "--data-dir", "-d",
        help = "Data directory (default: ~/.meshcore)"
    ).path().default(MeshConfig.defaultDataDir())

    override fun run() {
        val configFile = dataDir.resolve("meshcore.properties").toFile()
        val config = MeshConfig.load(if (configFile.exists()) configFile else null).copy(dataDir = dataDir)

        try {
            val apiUrl = "http://${config.apiHost}:${config.apiPort}/api/v1/messages/send"
            val body = """{"toPeerId":"$toPeerId","content":"${message.replace("\"", "\\\"")}"}"""

            val url = URL(apiUrl)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write(body.encodeToByteArray()) }

            val status = conn.responseCode
            val response = if (status < 400) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "error"
            }

            if (status in 200..299) {
                echo("✓ Message queued: $response")
            } else {
                echo("✗ Failed ($status): $response")
            }
        } catch (e: Exception) {
            echo("⚠ Could not connect to API: ${e.message}")
            echo("  Is the node running? Try: meshcore serve")
        }
    }
}
