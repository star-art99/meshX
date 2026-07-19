package io.meshcore.network.transport

import kotlinx.coroutines.flow.Flow

/**
 * Represents the result of a transport-level send operation.
 */
sealed class SendResult {
    object Success : SendResult()
    data class Failure(val reason: String, val cause: Throwable? = null) : SendResult()
}

/**
 * A packet received from the network transport layer.
 */
data class InboundPacket(
    val sourceAddress: String,
    val sourcePort: Int,
    val payload: ByteArray
)

/**
 * Abstraction over a network transport (TCP, UDP, BLE, LoRa, etc.).
 *
 * Implementations must be safe for concurrent use.
 * Each transport handles its own framing/buffering.
 */
interface Transport {
    /** Human-readable transport name for diagnostics. */
    val name: String

    /** Transport type for capability negotiation. */
    val type: TransportType

    /** Whether this transport is currently active and ready. */
    val isActive: Boolean

    /**
     * Start the transport, begin listening for inbound data.
     */
    suspend fun start()

    /**
     * Stop the transport and release all resources.
     */
    suspend fun stop()

    /**
     * Send [data] to the given [address]:[port].
     */
    suspend fun send(address: String, port: Int, data: ByteArray): SendResult

    /**
     * Flow of inbound packets received on this transport.
     */
    fun inboundPackets(): Flow<InboundPacket>
}

/**
 * Known transport types for capability negotiation and routing decisions.
 */
enum class TransportType {
    TCP,
    UDP,
    BLUETOOTH,
    BLUETOOTH_LE,
    WIFI_DIRECT,
    USB_SERIAL,
    LORA,
    QUIC,
    WIREGUARD,
    UNKNOWN
}
