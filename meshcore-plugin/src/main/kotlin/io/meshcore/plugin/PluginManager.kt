package io.meshcore.plugin

import io.meshcore.core.lifecycle.MeshComponent
import io.meshcore.core.logging.MeshLogger

private val log = MeshLogger.getLogger("io.meshcore.plugin")

/**
 * Plugin capability flags.
 */
enum class PluginCapability {
    MESSAGING,
    FILE_TRANSFER,
    NETWORKING,
    SENSOR_ACCESS,
    LOCATION,
    CAMERA,
    AUDIO
}

/**
 * A MeshCore plugin descriptor.
 */
data class PluginDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val capabilities: Set<PluginCapability>
)

/**
 * Plugin lifecycle interface.
 * All plugins must implement this.
 */
interface MeshPlugin : MeshComponent {
    val descriptor: PluginDescriptor
}

/**
 * Plugin registry and manager.
 *
 * TODO: Implement plugin sandbox (ClassLoader isolation + capability enforcement).
 * TODO: Implement plugin marketplace / discovery.
 * TODO: Implement plugin hot-reload.
 */
class PluginManager : MeshComponent {

    override val name: String = "PluginManager"
    private val plugins = mutableMapOf<String, MeshPlugin>()

    override suspend fun start() {
        log.info("Plugin manager started (stub)")
    }

    override suspend fun stop() {
        plugins.values.reversed().forEach { plugin ->
            try { plugin.stop() } catch (e: Exception) {
                log.warn("Error stopping plugin {}: {}", plugin.descriptor.id, e.message)
            }
        }
        plugins.clear()
        log.info("Plugin manager stopped")
    }

    fun register(plugin: MeshPlugin) {
        plugins[plugin.descriptor.id] = plugin
        log.info("Registered plugin: {} v{}", plugin.descriptor.name, plugin.descriptor.version)
    }

    fun listPlugins(): List<PluginDescriptor> = plugins.values.map { it.descriptor }
}
