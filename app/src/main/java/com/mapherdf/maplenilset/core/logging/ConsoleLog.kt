package com.mapherdf.maplenilset.core.logging

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LogLevel(val label: String) {
    DEBUG("DEBUG"),
    INFO("INFO"),
    SUCCESS("OK"),
    WARNING("WARN"),
    ERROR("ERROR"),
}

data class ConsoleEntry(
    val message: String,
    val level: LogLevel = LogLevel.INFO,
    val timestampMillis: Long = System.currentTimeMillis(),
) {
    fun displayText(): String {
        val timestamp = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(timestampFormatter)
        return "$timestamp [${level.label}] $message"
    }

    private companion object {
        private val timestampFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    }
}
