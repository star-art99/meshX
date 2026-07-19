package io.meshcore.routing

import io.meshcore.core.events.EventBus
import io.meshcore.core.events.PeerDiscoveredEvent
import io.meshcore.core.events.PeerLostEvent
import io.meshcore.core.lifecycle.MeshComponent
import io.meshcore.core.logging.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private val log = MeshLogger.getLogger("io.meshcore.routing")

/**
 * Routing table entry.
 */
data class Route(
    val destinationId: String,
    val nextHopAddress: String,
    val metric: Int, // lower = better
    val transport: String
)

/**
 * Mesh routing manager.
 *
 * Maintains a distributed routing table and selects optimal paths
 * for outbound messages.
 *
 * TODO: Implement full mesh routing protocol (e.g., OLSR or Babel variant):
 *   - Link-state or distance-vector updates
 *   - Loop detection via sequence numbers
 *   - Multi-path selection with load balancing
 *   - Store-and-forward for offline peers
 *
 * Current implementation: flat peer table (single-hop only).
 */
class MeshRouter(
    private val eventBus: EventBus
) : MeshComponent {

    override val name: String = "MeshRouter"

    private val routeTable = ConcurrentHashMap<String, Route>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun start() {
        log.info("Mesh router started")
        // Subscribe to peer discovery events to update routing table
        scope.launch {
            eventBus.subscribe<PeerDiscoveredEvent>().collect { event ->
                addRoute(Route(event.peerId, event.address, 1, "TCP"))
            }
        }
        scope.launch {
            eventBus.subscribe<PeerLostEvent>().collect { event ->
                removeRoute(event.peerId)
            }
        }
    }

    override suspend fun stop() {
        routeTable.clear()
        log.info("Mesh router stopped")
    }

    fun addRoute(route: Route) {
        routeTable[route.destinationId] = route
        log.debug("Route added: {} → {}", route.destinationId, route.nextHopAddress)
    }

    fun removeRoute(destinationId: String) {
        routeTable.remove(destinationId)
        log.debug("Route removed: {}", destinationId)
    }

    /**
     * Find the best route to [destinationId].
     * Returns null if no route is known.
     */
    fun findRoute(destinationId: String): Route? = routeTable[destinationId]

    fun allRoutes(): List<Route> = routeTable.values.toList()
}
