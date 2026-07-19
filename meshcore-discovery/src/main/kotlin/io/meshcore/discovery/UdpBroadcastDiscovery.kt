package io.meshcore.discovery

import io.meshcore.core.events.EventBus
import io.meshcore.core.events.PeerDiscoveredEvent
import io.meshcore.core.lifecycle.MeshComponent
import io.meshcore.core.logging.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

private val log = MeshLogger.getLogger("io.meshcore.discovery")

/**
 * Discovered peer information from broadcast.
 */
data class DiscoveredPeer(
    val nodeId: String,
    val publicKey: String,
    val address: String,
    val port: Int,
    val discoveredAt: Long = System.currentTimeMillis()
)

/**
 * UDP broadcast-based local peer discovery.
 *
 * This component periodically broadcasts a "hello" packet on the local network
 * and listens for similar announcements from other MeshCore nodes.
 *
 * Protocol:
 *   Beacon format: "MESHCORE|<nodeId>|<publicKeyHex>|<tcpPort>"
 *   Broadcast address: 255.255.255.255
 *
 * Security: Beacon packets are NOT authenticated (they are public announcements).
 * Actual peer authentication happens during the handshake in meshcore-network.
 *
 * TODO: Add mDNS-SD (DNS-SD over multicast) as a secondary discovery mechanism
 *       for environments where UDP broadcast is blocked (e.g., routed networks).
 *       Reference: RFC 6762 (mDNS), RFC 6763 (DNS-SD)
 */
class UdpBroadcastDiscovery(
    private val nodeId: String,
    private val publicKeyHex: String,
    private val tcpPort: Int,
    private val broadcastPort: Int = 7471,
    private val beaconIntervalMs: Long = 10_000L,
    private val eventBus: EventBus
) : MeshComponent {

    override val name: String = "UdpBroadcastDiscovery"

    private val _running = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var listenerJob: Job? = null
    private var beaconJob: Job? = null
    private var listenerSocket: DatagramSocket? = null

    override suspend fun start() {
        if (_running.getAndSet(true)) return
        log.info("Starting UDP broadcast discovery on port {}", broadcastPort)
        startListener()
        startBeacon()
    }

    override suspend fun stop() {
        _running.set(false)
        listenerJob?.cancel()
        beaconJob?.cancel()
        listenerSocket?.close()
        listenerSocket = null
        log.info("UDP broadcast discovery stopped")
    }

    private fun startListener() {
        listenerJob = scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val sock = DatagramSocket(broadcastPort)
                    sock.setBroadcast(true)
                    listenerSocket = sock
                    val buf = ByteArray(512)
                    while (isActive && !sock.isClosed) {
                        val packet = DatagramPacket(buf, buf.size)
                        try {
                            sock.receive(packet)
                            val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                            handleBeacon(msg, packet.address.hostAddress ?: "")
                        } catch (e: Exception) {
                            if (_running.get()) log.debug("Discovery listener error: {}", e.message)
                        }
                    }
                } catch (e: Exception) {
                    if (_running.get()) log.error("Discovery listener failed to start", e)
                }
            }
        }
    }

    private fun startBeacon() {
        beaconJob = scope.launch {
            val beacon = buildBeacon()
            val beaconBytes = beacon.encodeToByteArray()
            val broadcastAddr = InetAddress.getByName("255.255.255.255")

            while (isActive) {
                withContext(Dispatchers.IO) {
                    try {
                        DatagramSocket().use { sock ->
                            sock.setBroadcast(true)
                            val packet = DatagramPacket(beaconBytes, beaconBytes.size, broadcastAddr, broadcastPort)
                            sock.send(packet)
                            log.debug("Sent discovery beacon")
                        }
                    } catch (e: Exception) {
                        log.debug("Failed to send beacon: {}", e.message)
                    }
                }
                delay(beaconIntervalMs)
            }
        }
    }

    private fun buildBeacon(): String =
        "MESHCORE|$nodeId|$publicKeyHex|$tcpPort"

    private suspend fun handleBeacon(message: String, sourceAddress: String) {
        if (!message.startsWith("MESHCORE|")) return
        val parts = message.split("|")
        if (parts.size != 4) return
        val (_, remoteNodeId, remotePublicKey, tcpPortStr) = parts

        // Ignore our own beacon
        if (remoteNodeId == nodeId) return

        val tcpPort = tcpPortStr.toIntOrNull() ?: return
        log.info("Discovered peer: {} at {}:{}", remoteNodeId, sourceAddress, tcpPort)

        eventBus.publish(PeerDiscoveredEvent(remoteNodeId, "$sourceAddress:$tcpPort"))
    }
}
