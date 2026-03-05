// by Claude - log entry model for AI driver log capture
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.Serializable

/** A single captured log entry from the app. */
@Serializable
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String
)

@Serializable
enum class LogLevel { Log, Info, Warn, Error }
