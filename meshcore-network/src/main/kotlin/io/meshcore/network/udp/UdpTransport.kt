package io.meshcore.network.udp

import io.meshcore.core.logging.MeshLogger
import io.meshcore.network.transport.InboundPacket
import io.meshcore.network.transport.SendResult
import io.meshcore.network.transport.Transport
import io.meshcore.network.transport.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

private val log = MeshLogger.getLogger("io.meshcore.network.udp")

/**
 * UDP transport adapter.
 *
 * Sends and receives UDP datagrams. Suitable for discovery broadcasts
 * and small control messages.
 *
 * Note: UDP does not guarantee delivery or ordering.
 * For reliable messaging, use TCP transport.
 */
class UdpTransport(
    private val listenPort: Int = 7481
) : Transport {

    override val name: String = "UDP"
    override val type: TransportType = TransportType.UDP

    private val _active = AtomicBoolean(false)
    override val isActive: Boolean get() = _active.get()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var socket: DatagramSocket? = null

    private val _inbound = MutableSharedFlow<InboundPacket>(
        replay = 0,
        extraBufferCapacity = 512,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun inboundPackets(): SharedFlow<InboundPacket> = _inbound.asSharedFlow()

    override suspend fun start() {
        withContext(Dispatchers.IO) {
            val sock = DatagramSocket(listenPort)
            sock.setBroadcast(true)
            socket = sock
            _active.set(true)
            log.info("UDP transport listening on port {}", listenPort)

            scope.launch {
                val buf = ByteArray(MAX_DATAGRAM_SIZE)
                while (isActive && !sock.isClosed) {
                    try {
                        val packet = DatagramPacket(buf, buf.size)
                        sock.receive(packet)
                        val payload = packet.data.copyOf(packet.length)
                        val source = packet.address.hostAddress ?: "unknown"
                        _inbound.emit(InboundPacket(source, packet.port, payload))
                    } catch (e: Exception) {
                        if (_active.get()) log.debug("UDP receive error: {}", e.message)
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        _active.set(false)
        socket?.close()
        socket = null
        log.info("UDP transport stopped")
    }

    override suspend fun send(address: String, port: Int, data: ByteArray): SendResult {
        require(data.size <= MAX_DATAGRAM_SIZE) { "UDP payload too large: ${data.size}" }
        return withContext(Dispatchers.IO) {
            try {
                val sock = socket ?: DatagramSocket()
                val addr = InetAddress.getByName(address)
                val packet = DatagramPacket(data, data.size, addr, port)
                sock.send(packet)
                SendResult.Success
            } catch (e: Exception) {
                log.warn("UDP send to {}:{} failed: {}", address, port, e.message)
                SendResult.Failure("UDP send failed: ${e.message}", e)
            }
        }
    }

    companion object {
        const val MAX_DATAGRAM_SIZE = 65507 // max UDP payload
    }
}
