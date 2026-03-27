package com.lightningkite.kiteui

/**
 * A circular buffer for log entries, used by the AI driver to provide recent log history.
 */
object LogBuffer {
    data class Entry(val timestamp: Double, val level: String, val tag: String, val msg: String)

    private val buffer = RingBuffer<Entry>(1000)

    fun add(level: String, tag: String, msg: String) {
        buffer.add(Entry(clockMillis(), level, tag, msg))
    }

    fun recent(count: Int = 50): List<Entry> = buffer.takeLast(count)

    fun clear() = buffer.clear()
}
