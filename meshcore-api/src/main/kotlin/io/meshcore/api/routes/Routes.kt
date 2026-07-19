package io.meshcore.api.routes

import io.meshcore.api.model.ErrorResponse
import io.meshcore.api.model.HealthResponse
import io.meshcore.api.model.NodeInfoResponse
import io.meshcore.api.model.PeerResponse
import io.meshcore.api.model.SendMessageRequest
import io.meshcore.api.model.SendMessageResponse
import io.meshcore.core.config.MeshConfig
import io.meshcore.core.errors.MeshError
import io.meshcore.storage.repository.PeerRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

/**
 * Register all API routes on the Ktor application.
 */
fun Application.configureRoutes(
    config: MeshConfig,
    peerRepository: PeerRepository,
    startTime: Long = System.currentTimeMillis()
) {
    routing {
        // Health check
        get("/health") {
            call.respond(HealthResponse())
        }

        route("/api/v1") {
            // Node information
            get("/node") {
                call.respond(
                    NodeInfoResponse(
                        nodeId = config.nodeId,
                        uptime = System.currentTimeMillis() - startTime,
                        apiPort = config.apiPort
                    )
                )
            }

            // Peer list
            get("/peers") {
                val peers = peerRepository.findAll().map { peer ->
                    PeerResponse(
                        id = peer.id,
                        publicKey = peer.publicKey,
                        alias = peer.alias,
                        address = peer.address,
                        lastSeenAt = peer.lastSeenAt?.toString(),
                        isTrusted = peer.isTrusted
                    )
                }
                call.respond(peers)
            }

            // Get specific peer
            get("/peers/{id}") {
                val id = call.parameters["id"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Missing peer id", "INVALID_REQUEST")
                    )
                val peer = peerRepository.findById(id)
                    ?: return@get call.respond(
                        HttpStatusCode.NotFound,
                        ErrorResponse("Peer not found: $id", "PEER_NOT_FOUND")
                    )
                call.respond(
                    PeerResponse(
                        id = peer.id,
                        publicKey = peer.publicKey,
                        alias = peer.alias,
                        address = peer.address,
                        lastSeenAt = peer.lastSeenAt?.toString(),
                        isTrusted = peer.isTrusted
                    )
                )
            }

            // Send message
            post("/messages/send") {
                val request = try {
                    call.receive<SendMessageRequest>()
                } catch (e: Exception) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid request body", "INVALID_REQUEST")
                    )
                }
                if (request.toPeerId.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("toPeerId must not be blank", "INVALID_REQUEST")
                    )
                }
                if (request.content.isBlank()) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("content must not be blank", "INVALID_REQUEST")
                    )
                }
                // TODO: delegate to meshcore-messenger for actual E2E send
                val messageId = UUID.randomUUID().toString()
                call.respond(HttpStatusCode.Accepted, SendMessageResponse(messageId = messageId))
            }
        }
    }
}
