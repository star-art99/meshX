package io.meshcore.core.lifecycle

import io.meshcore.core.config.MeshConfig
import io.meshcore.core.events.EventBus
import io.meshcore.core.events.NodeStartedEvent
import io.meshcore.core.events.NodeStoppedEvent
import io.meshcore.core.logging.MeshLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

private val log = MeshLogger.getLogger("io.meshcore.core.lifecycle")

/**
 * Lifecycle states for the MeshCore node.
 */
enum class NodeState {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}

/**
 * Component lifecycle interface – all major subsystems implement this.
 */
interface MeshComponent {
    val name: String
    suspend fun start()
    suspend fun stop()
}

/**
 * Central lifecycle manager for the MeshCore node.
 * Manages startup/shutdown order and provides a shared coroutine scope.
 */
class NodeLifecycle(
    val config: MeshConfig,
    val eventBus: EventBus = EventBus()
) {
    private val _state = AtomicReference(NodeState.STOPPED)
    val state: NodeState get() = _state.get()

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val components: CopyOnWriteArrayList<MeshComponent> = CopyOnWriteArrayList()

    /** Register a component to be managed by this lifecycle. */
    fun register(component: MeshComponent) {
        components.add(component)
        log.debug("Registered component: {}", component.name)
    }

    /** Start all registered components in registration order. */
    suspend fun start() {
        check(_state.compareAndSet(NodeState.STOPPED, NodeState.STARTING)) {
            "Node is not in STOPPED state, current: ${_state.get()}"
        }
        log.info("Starting MeshCore node [id={}]", config.nodeId)
        try {
            config.validate()
            for (component in components) {
                log.debug("Starting component: {}", component.name)
                component.start()
            }
            _state.set(NodeState.RUNNING)
            eventBus.publish(NodeStartedEvent(config.nodeId))
            log.info("MeshCore node started successfully [id={}]", config.nodeId)
        } catch (e: Exception) {
            _state.set(NodeState.ERROR)
            log.error("Failed to start node", e)
            throw e
        }
    }

    /** Stop all registered components in reverse order. */
    suspend fun stop() {
        if (!_state.compareAndSet(NodeState.RUNNING, NodeState.STOPPING)) {
            log.warn("Node stop requested but state is {}", _state.get())
            return
        }
        log.info("Stopping MeshCore node [id={}]", config.nodeId)
        for (component in components.reversed()) {
            try {
                log.debug("Stopping component: {}", component.name)
                component.stop()
            } catch (e: Exception) {
                log.error("Error stopping component {}", component.name, e)
            }
        }
        scope.cancel()
        _state.set(NodeState.STOPPED)
        eventBus.publish(NodeStoppedEvent(config.nodeId))
        log.info("MeshCore node stopped [id={}]", config.nodeId)
    }

    /** Register a JVM shutdown hook. */
    fun registerShutdownHook(block: suspend () -> Unit) {
        Runtime.getRuntime().addShutdownHook(Thread {
            kotlinx.coroutines.runBlocking { block() }
        })
    }
}
