package io.meshcore.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * MeshCore log levels, mapping to SLF4J levels.
 */
enum class LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARNING,
    ERROR
}

/**
 * Structured logging façade.
 * All modules use this to obtain loggers, ensuring consistent formatting.
 */
object MeshLogger {

    /**
     * Obtain a logger for the given name (typically the class or module name).
     */
    fun getLogger(name: String): MeshLog = MeshLog(LoggerFactory.getLogger(name))

    /**
     * Obtain a logger for the given class.
     */
    fun getLogger(clazz: Class<*>): MeshLog = MeshLog(LoggerFactory.getLogger(clazz))

    /**
     * Obtain a logger for the given Kotlin class.
     */
    inline fun <reified T : Any> getLogger(): MeshLog = getLogger(T::class.java)
}

/**
 * Thin wrapper over SLF4J Logger providing MeshCore-level API.
 */
class MeshLog(private val delegate: Logger) {

    fun trace(msg: String, vararg args: Any?) = delegate.trace(msg, *args)
    fun debug(msg: String, vararg args: Any?) = delegate.debug(msg, *args)
    fun info(msg: String, vararg args: Any?) = delegate.info(msg, *args)
    fun warn(msg: String, vararg args: Any?) = delegate.warn(msg, *args)
    fun error(msg: String, vararg args: Any?) = delegate.error(msg, *args)
    fun error(msg: String, cause: Throwable) = delegate.error(msg, cause)

    fun log(level: LogLevel, msg: String, vararg args: Any?) {
        when (level) {
            LogLevel.TRACE -> trace(msg, *args)
            LogLevel.DEBUG -> debug(msg, *args)
            LogLevel.INFO -> info(msg, *args)
            LogLevel.WARNING -> warn(msg, *args)
            LogLevel.ERROR -> error(msg, *args)
        }
    }

    val name: String get() = delegate.name
    val isTraceEnabled: Boolean get() = delegate.isTraceEnabled
    val isDebugEnabled: Boolean get() = delegate.isDebugEnabled
}

/**
 * Extension to get a logger for any class easily.
 */
inline fun <reified T : Any> T.logger(): MeshLog = MeshLogger.getLogger<T>()
