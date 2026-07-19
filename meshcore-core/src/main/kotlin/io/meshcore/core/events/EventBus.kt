package io.meshcore.core.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Marker interface for all internal MeshCore events.
 */
interface MeshEvent

/**
 * Internal event bus using Kotlin coroutines SharedFlow.
 * All modules subscribe to events via this bus.
 *
 * The bus is thread-safe and coroutine-friendly.
 * Slow subscribers are protected by a bounded replay buffer.
 */
class EventBus(
    @PublishedApi internal val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    @PublishedApi
    internal val _events = MutableSharedFlow<MeshEvent>(
        replay = 0,
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val events: SharedFlow<MeshEvent> = _events.asSharedFlow()

    /**
     * Publish an event to all current subscribers.
     * This is safe to call from any coroutine or thread.
     */
    suspend fun publish(event: MeshEvent) {
        _events.emit(event)
    }

    /**
     * Fire-and-forget event publishing from non-coroutine contexts.
     */
    fun publishAsync(event: MeshEvent) {
        scope.launch { _events.emit(event) }
    }

    /**
     * Subscribe to events of a specific type.
     * Returns a SharedFlow filtered to the given type.
     */
    inline fun <reified T : MeshEvent> subscribe(): SharedFlow<T> {
        val filtered = MutableSharedFlow<T>(
            replay = 0,
            extraBufferCapacity = 128,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        scope.launch {
            _events.collect { event ->
                if (event is T) filtered.emit(event)
            }
        }
        return filtered.asSharedFlow()
    }
}

// --- Standard system events ---

data class NodeStartedEvent(val nodeId: String) : MeshEvent
data class NodeStoppedEvent(val nodeId: String) : MeshEvent
data class PeerDiscoveredEvent(val peerId: String, val address: String) : MeshEvent
data class PeerLostEvent(val peerId: String) : MeshEvent
data class MessageReceivedEvent(val fromPeerId: String, val messageId: String) : MeshEvent
data class ErrorEvent(val error: Throwable, val context: String = "") : MeshEvent
data class ConfigChangedEvent(val key: String, val value: String) : MeshEvent
