package io.meshcore.web

import io.meshcore.core.lifecycle.MeshComponent
import io.meshcore.core.logging.MeshLogger

private val log = MeshLogger.getLogger("io.meshcore.web")

/**
 * Local web UI server.
 *
 * Serves the static web dashboard on localhost.
 * The dashboard communicates with the REST API (meshcore-api).
 *
 * TODO: Implement static file serving via Ktor StaticContent plugin.
 * TODO: Build dashboard UI with Kotlin/JS or minimal vanilla HTML/JS.
 * TODO: Add WebSocket support for real-time status updates.
 */
class WebServer : MeshComponent {

    override val name: String = "WebServer"

    override suspend fun start() {
        log.info("Web server started (stub – not yet implemented)")
    }

    override suspend fun stop() {
        log.info("Web server stopped")
    }
}
