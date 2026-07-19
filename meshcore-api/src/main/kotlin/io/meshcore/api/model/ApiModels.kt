package io.meshcore.api.model

import java.time.Instant

// --- Request/Response DTOs ---

data class NodeInfoResponse(
    val nodeId: String,
    val version: String = "0.1.0",
    val uptime: Long,
    val apiPort: Int
)

data class HealthResponse(
    val status: String = "ok",
    val timestamp: String = Instant.now().toString()
)

data class PeerResponse(
    val id: String,
    val publicKey: String,
    val alias: String?,
    val address: String?,
    val lastSeenAt: String?,
    val isTrusted: Boolean
)

data class SendMessageRequest(
    val toPeerId: String,
    val content: String
)

data class SendMessageResponse(
    val messageId: String,
    val status: String = "queued",
    val sentAt: String = Instant.now().toString()
)

data class ErrorResponse(
    val error: String,
    val code: String,
    val timestamp: String = Instant.now().toString()
)
