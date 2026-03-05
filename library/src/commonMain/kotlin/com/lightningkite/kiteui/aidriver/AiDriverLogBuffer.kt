// by Claude - circular log buffer that wraps LogRoot to capture app logs for the AI driver
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.clockMillis

/**
 * Intercepts all [Log] calls, stores them in a circular buffer, and forwards to the original logger.
 * Installed by [AiDriver.connect] so the daemon can pull logs on demand.
 */
object AiDriverLogBuffer : Log {
    private const val MAX_ENTRIES = 1000
    private val buffer = ArrayDeque<LogEntry>(MAX_ENTRIES)
    private var delegate: Log? = null

    fun install(original: Log) {
        delegate = original
    }

    fun entries(count: Int): List<LogEntry> {
        val n = count.coerceAtMost(buffer.size)
        return buffer.toList().takeLast(n)
    }

    private fun record(level: LogLevel, tag: String, entries: Array<out Any?>) {
        val msg = entries.joinToString(" ") { it.toString() }
        if (buffer.size >= MAX_ENTRIES) buffer.removeFirst()
        buffer.addLast(LogEntry(clockMillis().toLong(), level, tag, msg))
    }

    override fun tag(tag: String): Log = TaggedBuffer(tag)
    override fun log(vararg entries: Any?) { record(LogLevel.Log, "", entries); delegate?.log(*entries) }
    override fun error(vararg entries: Any?) { record(LogLevel.Error, "", entries); delegate?.error(*entries) }
    override fun info(vararg entries: Any?) { record(LogLevel.Info, "", entries); delegate?.info(*entries) }
    override fun warn(vararg entries: Any?) { record(LogLevel.Warn, "", entries); delegate?.warn(*entries) }

    private class TaggedBuffer(val tag: String) : Log {
        override fun tag(tag: String) = TaggedBuffer("${this.tag}/$tag")
        override fun log(vararg entries: Any?) { record(LogLevel.Log, tag, entries); delegate?.tag(tag)?.log(*entries) }
        override fun error(vararg entries: Any?) { record(LogLevel.Error, tag, entries); delegate?.tag(tag)?.error(*entries) }
        override fun info(vararg entries: Any?) { record(LogLevel.Info, tag, entries); delegate?.tag(tag)?.info(*entries) }
        override fun warn(vararg entries: Any?) { record(LogLevel.Warn, tag, entries); delegate?.tag(tag)?.warn(*entries) }
    }
}
