package io.meshcore.api.server

import io.meshcore.core.config.MeshConfig
import io.meshcore.core.lifecycle.MeshComponent
import io.meshcore.core.logging.MeshLogger
import io.meshcore.api.routes.configureRoutes
import io.meshcore.storage.repository.PeerRepository
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

private val log = MeshLogger.getLogger("io.meshcore.api.server")

/**
 * MeshCore REST API server backed by Ktor/Netty.
 */
class ApiServer(
    private val config: MeshConfig,
    private val peerRepository: PeerRepository
) : MeshComponent {

    override val name: String = "ApiServer"
    private val startTime = System.currentTimeMillis()
    private var engine: io.ktor.server.engine.ApplicationEngine? = null

    override suspend fun start() {
        log.info("Starting API server on {}:{}", config.apiHost, config.apiPort)
        engine = embeddedServer(
            Netty,
            host = config.apiHost,
            port = config.apiPort,
            module = {
                configureMeshApiPlugins()
                configureRoutes(config, peerRepository, startTime)
            }
        ).start(wait = false)
        log.info("API server started at http://{}:{}", config.apiHost, config.apiPort)
    }

    override suspend fun stop() {
        engine?.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
        log.info("API server stopped")
    }
}
