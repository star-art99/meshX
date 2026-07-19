package io.meshcore.core.config

import io.meshcore.core.errors.ConfigurationError
import java.io.File
import java.nio.file.Path
import java.util.Properties

/**
 * MeshCore node configuration.
 * Values are loaded from a properties file and can be overridden by environment variables.
 *
 * Environment variable mapping: CONFIG_KEY → config.key  (uppercase, dots become underscores)
 * e.g. ENV var MESHCORE_NODE_ID overrides config key meshcore.node.id
 */
data class MeshConfig(
    val nodeId: String,
    val dataDir: Path,
    val apiHost: String,
    val apiPort: Int,
    val logLevel: String,
    val discoveryEnabled: Boolean,
    val discoveryBroadcastPort: Int,
    val maxPeers: Int,
    val extra: Map<String, String> = emptyMap()
) {
    companion object {
        /** Default data directory – $HOME/.meshcore */
        fun defaultDataDir(): Path =
            Path.of(System.getProperty("user.home"), ".meshcore")

        /**
         * Load configuration from a file (optional) with environment variable overrides.
         * If [configFile] does not exist, defaults are used.
         */
        fun load(configFile: File? = null): MeshConfig {
            val props = Properties()

            // Load from file if provided
            if (configFile != null && configFile.exists()) {
                configFile.inputStream().use { props.load(it) }
            }

            // Override with environment variables (MESHCORE_* prefix)
            System.getenv()
                .filter { (k, _) -> k.startsWith("MESHCORE_") }
                .forEach { (envKey, value) ->
                    val propKey = envKey
                        .removePrefix("MESHCORE_")
                        .lowercase()
                        .replace('_', '.')
                    props["meshcore.$propKey"] = value
                }

            fun str(key: String, default: String): String =
                props.getProperty(key, default)

            fun int(key: String, default: Int): Int =
                props.getProperty(key)?.toIntOrNull() ?: default

            fun bool(key: String, default: Boolean): Boolean =
                props.getProperty(key)?.toBooleanStrictOrNull() ?: default

            val dataDir = Path.of(str("meshcore.data.dir", defaultDataDir().toString()))

            // Node ID: persist once generated, load from config or auto-generate
            val nodeId = str("meshcore.node.id", "").ifBlank { generateNodeAlias() }

            val extra = props.entries
                .map { it.key.toString() to it.value.toString() }
                .filter { (k, _) ->
                    !k.startsWith("meshcore.node.id") &&
                    !k.startsWith("meshcore.data.dir") &&
                    !k.startsWith("meshcore.api.") &&
                    !k.startsWith("meshcore.log.") &&
                    !k.startsWith("meshcore.discovery.")
                }
                .toMap()

            return MeshConfig(
                nodeId = nodeId,
                dataDir = dataDir,
                apiHost = str("meshcore.api.host", "127.0.0.1"),
                apiPort = int("meshcore.api.port", 7470),
                logLevel = str("meshcore.log.level", "INFO"),
                discoveryEnabled = bool("meshcore.discovery.enabled", true),
                discoveryBroadcastPort = int("meshcore.discovery.broadcast.port", 7471),
                maxPeers = int("meshcore.max.peers", 64),
                extra = extra
            )
        }

        private fun generateNodeAlias(): String {
            val chars = ('a'..'z') + ('0'..'9')
            return "node-" + (1..8).map { chars.random() }.joinToString("")
        }
    }

    /** Validate mandatory configuration constraints */
    fun validate() {
        if (apiPort !in 1..65535) throw ConfigurationError("meshcore.api.port", "Must be 1-65535")
        if (discoveryBroadcastPort !in 1..65535) throw ConfigurationError("meshcore.discovery.broadcast.port", "Must be 1-65535")
        if (maxPeers < 1) throw ConfigurationError("meshcore.max.peers", "Must be >= 1")
    }

    /** Write the current configuration to a file */
    fun saveTo(file: File) {
        file.parentFile?.mkdirs()
        val props = Properties()
        props["meshcore.node.id"] = nodeId
        props["meshcore.data.dir"] = dataDir.toString()
        props["meshcore.api.host"] = apiHost
        props["meshcore.api.port"] = apiPort.toString()
        props["meshcore.log.level"] = logLevel
        props["meshcore.discovery.enabled"] = discoveryEnabled.toString()
        props["meshcore.discovery.broadcast.port"] = discoveryBroadcastPort.toString()
        props["meshcore.max.peers"] = maxPeers.toString()
        extra.forEach { (k, v) -> props[k] = v }
        file.outputStream().use { props.store(it, "MeshCore configuration") }
    }
}
