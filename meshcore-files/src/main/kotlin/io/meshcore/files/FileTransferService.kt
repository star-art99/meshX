package io.meshcore.files

import io.meshcore.core.logging.MeshLogger
import java.io.File
import java.security.MessageDigest

private val log = MeshLogger.getLogger("io.meshcore.files")

const val DEFAULT_CHUNK_SIZE = 256 * 1024 // 256 KB per chunk

/**
 * A file transfer session with chunking support.
 */
data class FileTransfer(
    val id: String,
    val fileName: String,
    val totalSize: Long,
    val sha256Hash: String,
    val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    var bytesTransferred: Long = 0
) {
    val totalChunks: Int get() = ((totalSize + chunkSize - 1) / chunkSize).toInt()
    val isComplete: Boolean get() = bytesTransferred >= totalSize
}

/**
 * File transfer service with chunking, resume, and integrity verification.
 *
 * TODO: Implement full transfer protocol:
 *   - Chunked upload/download with resume support
 *   - Per-chunk SHA-256 integrity check
 *   - E2E encryption of file chunks
 *   - Compression for compressible files
 *   - Bandwidth throttling
 */
class FileTransferService {

    /**
     * Compute SHA-256 hash of [file] for integrity verification.
     */
    fun computeHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(DEFAULT_CHUNK_SIZE)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Prepare a file for transfer, computing metadata.
     */
    fun prepareTransfer(file: File): FileTransfer {
        require(file.exists()) { "File does not exist: ${file.absolutePath}" }
        val hash = computeHash(file)
        val id = java.util.UUID.randomUUID().toString()
        log.info("Prepared file transfer: {} ({} bytes, sha256={})", file.name, file.length(), hash.take(12))
        return FileTransfer(
            id = id,
            fileName = file.name,
            totalSize = file.length(),
            sha256Hash = hash
        )
    }

    /**
     * Read a specific chunk from a file.
     */
    fun readChunk(file: File, chunkIndex: Int, chunkSize: Int = DEFAULT_CHUNK_SIZE): ByteArray {
        val offset = chunkIndex.toLong() * chunkSize
        require(offset < file.length()) { "Chunk index out of range" }
        file.inputStream().use { stream ->
            stream.skip(offset)
            val buf = ByteArray(chunkSize)
            val read = stream.read(buf)
            return if (read < chunkSize) buf.copyOf(read) else buf
        }
    }
}
