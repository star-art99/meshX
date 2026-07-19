package io.meshcore.core

import io.meshcore.core.config.MeshConfig
import io.meshcore.core.errors.*
import io.meshcore.core.events.EventBus
import io.meshcore.core.events.NodeStartedEvent
import io.meshcore.core.lifecycle.NodeLifecycle
import io.meshcore.core.lifecycle.NodeState
import io.meshcore.core.logging.MeshLogger
import io.meshcore.core.logging.LogLevel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MeshConfigTest {

    @Test
    fun `load defaults when no file provided`() {
        val config = MeshConfig.load(null)
        assertEquals("127.0.0.1", config.apiHost)
        assertEquals(7470, config.apiPort)
        assertTrue(config.discoveryEnabled)
        assertTrue(config.nodeId.isNotBlank())
    }

    @Test
    fun `validate passes with valid config`() {
        val config = MeshConfig.load(null)
        config.validate() // should not throw
    }

    @Test
    fun `validate fails with invalid port`() {
        val config = MeshConfig.load(null).copy(apiPort = 0)
        assertThrows<ConfigurationError> { config.validate() }
    }

    @Test
    fun `saveTo and reload preserves values`() {
        val original = MeshConfig.load(null).copy(nodeId = "test-node-123")
        val tmpFile = createTempFile("meshcore-test", ".properties")
        tmpFile.deleteOnExit()
        original.saveTo(tmpFile)

        val loaded = MeshConfig.load(tmpFile)
        assertEquals("test-node-123", loaded.nodeId)
        assertEquals(original.apiHost, loaded.apiHost)
        assertEquals(original.apiPort, loaded.apiPort)
    }
}

class MeshLoggerTest {

    @Test
    fun `logger is created for name`() {
        val log = MeshLogger.getLogger("test-logger")
        assertEquals("test-logger", log.name)
    }

    @Test
    fun `logger logs at all levels without error`() {
        val log = MeshLogger.getLogger("test")
        log.log(LogLevel.TRACE, "trace message")
        log.log(LogLevel.DEBUG, "debug message")
        log.log(LogLevel.INFO, "info message")
        log.log(LogLevel.WARNING, "warning message")
        log.log(LogLevel.ERROR, "error message")
    }
}

class EventBusTest {

    @Test
    fun `publish and receive event`() = runBlocking<Unit> {
        val bus = EventBus()
        var received = false

        val job = launch {
            bus.subscribe<NodeStartedEvent>().collect {
                received = true
            }
        }
        kotlinx.coroutines.delay(50)
        bus.publish(NodeStartedEvent("test-node"))
        kotlinx.coroutines.delay(100)
        assertTrue(received)
        job.cancel()
    }

    @Test
    fun `publishAsync does not throw`() {
        val bus = EventBus()
        bus.publishAsync(NodeStartedEvent("test-node"))
    }
}

class NodeLifecycleTest {

    @Test
    fun `initial state is STOPPED`() {
        val config = MeshConfig.load(null)
        val lifecycle = NodeLifecycle(config)
        assertEquals(NodeState.STOPPED, lifecycle.state)
    }

    @Test
    fun `start transitions to RUNNING`() = runBlocking {
        val config = MeshConfig.load(null)
        val lifecycle = NodeLifecycle(config)
        lifecycle.start()
        assertEquals(NodeState.RUNNING, lifecycle.state)
        lifecycle.stop()
    }

    @Test
    fun `stop transitions to STOPPED`() = runBlocking {
        val config = MeshConfig.load(null)
        val lifecycle = NodeLifecycle(config)
        lifecycle.start()
        lifecycle.stop()
        assertEquals(NodeState.STOPPED, lifecycle.state)
    }
}

class MeshErrorTest {

    @Test
    fun `IdentityNotFoundError has correct code`() {
        val error = IdentityNotFoundError("user123")
        assertEquals("IDENTITY_NOT_FOUND", error.code)
        assertTrue(error.message.contains("user123"))
    }

    @Test
    fun `StorageError wraps cause`() {
        val cause = RuntimeException("disk full")
        val error = StorageError("write failed", cause)
        assertEquals("STORAGE_ERROR", error.code)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `meshTry captures exception`() {
        val result = meshTry { throw RuntimeException("boom") }
        assertTrue(result.isFailure)
    }

    @Test
    fun `meshTry returns success`() {
        val result = meshTry { 42 }
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrNull())
    }
}
