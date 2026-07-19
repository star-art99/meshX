package io.meshcore.core.errors

/**
 * Base class for all MeshCore domain errors.
 * Each error carries a code (for programmatic handling) and a message.
 */
sealed class MeshError(
    val code: String,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

// --- Identity errors ---

class IdentityNotFoundError(id: String) : MeshError(
    code = "IDENTITY_NOT_FOUND",
    message = "Identity not found: $id"
)

class IdentityAlreadyExistsError(id: String) : MeshError(
    code = "IDENTITY_ALREADY_EXISTS",
    message = "Identity already exists: $id"
)

class InvalidIdentityError(reason: String) : MeshError(
    code = "INVALID_IDENTITY",
    message = "Invalid identity: $reason"
)

// --- Crypto errors ---

class SignatureVerificationError(message: String = "Signature verification failed") : MeshError(
    code = "SIGNATURE_VERIFICATION_FAILED",
    message = message
)

class EncryptionError(reason: String, cause: Throwable? = null) : MeshError(
    code = "ENCRYPTION_ERROR",
    message = "Encryption failed: $reason",
    cause = cause
)

class DecryptionError(reason: String, cause: Throwable? = null) : MeshError(
    code = "DECRYPTION_ERROR",
    message = "Decryption failed: $reason",
    cause = cause
)

class KeyGenerationError(reason: String, cause: Throwable? = null) : MeshError(
    code = "KEY_GENERATION_ERROR",
    message = "Key generation failed: $reason",
    cause = cause
)

// --- Network errors ---

class ConnectionError(address: String, cause: Throwable? = null) : MeshError(
    code = "CONNECTION_ERROR",
    message = "Failed to connect to $address",
    cause = cause
)

class TransportError(reason: String, cause: Throwable? = null) : MeshError(
    code = "TRANSPORT_ERROR",
    message = "Transport error: $reason",
    cause = cause
)

class PeerNotFoundError(peerId: String) : MeshError(
    code = "PEER_NOT_FOUND",
    message = "Peer not found: $peerId"
)

// --- Storage errors ---

class StorageError(reason: String, cause: Throwable? = null) : MeshError(
    code = "STORAGE_ERROR",
    message = "Storage error: $reason",
    cause = cause
)

class MigrationError(version: Int, cause: Throwable? = null) : MeshError(
    code = "MIGRATION_ERROR",
    message = "Database migration failed at version $version",
    cause = cause
)

// --- Config errors ---

class ConfigurationError(key: String, reason: String) : MeshError(
    code = "CONFIGURATION_ERROR",
    message = "Configuration error for '$key': $reason"
)

// --- Message errors ---

class MessageTooLargeError(sizeBytes: Long, maxBytes: Long) : MeshError(
    code = "MESSAGE_TOO_LARGE",
    message = "Message size $sizeBytes bytes exceeds maximum $maxBytes bytes"
)

class InvalidMessageError(reason: String) : MeshError(
    code = "INVALID_MESSAGE",
    message = "Invalid message: $reason"
)

// --- Result type helpers ---

typealias MeshResult<T> = Result<T>

inline fun <T> meshTry(block: () -> T): MeshResult<T> = runCatching(block)
