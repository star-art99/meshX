package io.meshcore.network.tcp

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
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

private val log = MeshLogger.getLogger("io.meshcore.network.tcp")

/**
 * TCP transport adapter.
 *
 * Listens on [listenPort] for inbound connections.
 * Each inbound connection is read in full (framed with a 4-byte big-endian length prefix).
 *
 * Frame format: [4-byte length][payload bytes]
 */
class TcpTransport(
    private val listenPort: Int = 7480
) : Transport {

    override val name: String = "TCP"
    override val type: TransportType = TransportType.TCP

    private val _active = AtomicBoolean(false)
    override val isActive: Boolean get() = _active.get()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    private val _inbound = MutableSharedFlow<InboundPacket>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override fun inboundPackets(): SharedFlow<InboundPacket> = _inbound.asSharedFlow()

    override suspend fun start() {
        withContext(Dispatchers.IO) {
            val ss = ServerSocket(listenPort)
            serverSocket = ss
            _active.set(true)
            log.info("TCP transport listening on port {}", listenPort)

            scope.launch {
                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        launch { handleInbound(clientSocket) }
                    } catch (e: SocketException) {
                        if (_active.get()) log.warn("TCP accept error: {}", e.message)
                    } catch (e: Exception) {
                        log.error("TCP accept exception", e)
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        _active.set(false)
        serverSocket?.close()
        serverSocket = null
        log.info("TCP transport stopped")
    }

    override suspend fun send(address: String, port: Int, data: ByteArray): SendResult {
        return withContext(Dispatchers.IO) {
            try {
                Socket(address, port).use { socket ->
                    socket.soTimeout = 5000
                    val out = DataOutputStream(socket.getOutputStream())
                    out.writeInt(data.size)
                    out.write(data)
                    out.flush()
                }
                SendResult.Success
            } catch (e: Exception) {
                log.warn("TCP send to {}:{} failed: {}", address, port, e.message)
                SendResult.Failure("Send failed: ${e.message}", e)
            }
        }
    }

    private suspend fun handleInbound(socket: Socket) {
        val remoteAddr = socket.inetAddress.hostAddress ?: "unknown"
        val remotePort = socket.port
        try {
            socket.use {
                socket.soTimeout = 10000
                val input = DataInputStream(socket.getInputStream())
                val length = input.readInt()
                if (length <= 0 || length > MAX_PACKET_SIZE) {
                    log.warn("TCP: invalid packet length {} from {}:{}", length, remoteAddr, remotePort)
                    return
                }
                val payload = ByteArray(length)
                input.readFully(payload)
                _inbound.emit(InboundPacket(remoteAddr, remotePort, payload))
            }
        } catch (e: Exception) {
            if (_active.get()) log.debug("TCP: connection error from {}:{}: {}", remoteAddr, remotePort, e.message)
        }
    }

    companion object {
        const val MAX_PACKET_SIZE = 10 * 1024 * 1024 // 10 MB
    }
}
