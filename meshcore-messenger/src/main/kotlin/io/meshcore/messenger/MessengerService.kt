package io.meshcore.messenger

import io.meshcore.core.errors.InvalidMessageError
import io.meshcore.core.errors.MessageTooLargeError
import io.meshcore.core.logging.MeshLogger

private val log = MeshLogger.getLogger("io.meshcore.messenger")

const val MAX_MESSAGE_BYTES = 1024 * 1024L // 1 MB

/**
 * Message types supported by the messenger.
 */
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    DOCUMENT,
    AUDIO,
    REACTION,
    SYSTEM
}

/**
 * An outbound message prepared for sending.
 */
data class OutboundMessage(
    val id: String,
    val toPeerId: String,
    val type: MessageType,
    val content: ByteArray,
    val replyToId: String? = null
)

/**
 * Messenger service.
 *
 * Handles E2E encryption, delivery tracking, and store-and-forward for messages.
 *
 * TODO: Implement full E2E encrypted messaging:
 *   - Double-Ratchet algorithm for forward secrecy
 *   - Sealed sender to hide metadata
 *   - Group message fanout with shared group key
 *   - Read receipts and delivery receipts
 *   - Self-destructing messages with timer
 */
class MessengerService {

    fun validateMessage(toPeerId: String, content: ByteArray) {
        if (toPeerId.isBlank()) throw InvalidMessageError("toPeerId must not be blank")
        if (content.isEmpty()) throw InvalidMessageError("message content must not be empty")
        if (content.size > MAX_MESSAGE_BYTES) {
            throw MessageTooLargeError(content.size.toLong(), MAX_MESSAGE_BYTES)
        }
    }

    fun prepareOutbound(
        toPeerId: String,
        content: ByteArray,
        type: MessageType = MessageType.TEXT
    ): OutboundMessage {
        validateMessage(toPeerId, content)
        val id = java.util.UUID.randomUUID().toString()
        log.debug("Prepared outbound message {} to {}", id, toPeerId)
        return OutboundMessage(id, toPeerId, type, content)
    }
}
