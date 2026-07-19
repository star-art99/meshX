package io.meshcore.storage.repository

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Parse an Instant from a string that may be either:
 * - ISO-8601 format: "2024-01-01T12:00:00Z"  (from Kotlin Instant.toString())
 * - SQLite datetime format: "2024-01-01 12:00:00"  (from datetime('now'))
 */
internal fun parseInstant(value: String): Instant {
    return if (value.contains('T')) {
        // ISO-8601 with Z or offset
        if (value.endsWith('Z') || value.contains('+')) {
            Instant.parse(value)
        } else {
            Instant.parse("${value}Z")
        }
    } else {
        // SQLite format: "YYYY-MM-DD HH:MM:SS"
        LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            .toInstant(ZoneOffset.UTC)
    }
}
