package com.lightningkite.kiteui

import kotlinx.coroutines.sync.Mutex

/**
 * A circular buffer for log entries, used by the AI driver to provide recent log history.
 */
object LogBuffer {
    data class Entry(val timestamp: Double, val level: String, val tag: String, val msg: String)

    private val buffer = ArrayDeque<Entry>()
    private const val MAX_SIZE = 1000
    private val mutex = Mutex()

    fun add(level: String, tag: String, msg: String) {
        val entry = Entry(clockMillis(), level, tag, msg)
        if (!mutex.tryLock()) return
        try {
            if (buffer.size >= MAX_SIZE) buffer.removeFirst()
            buffer.addLast(entry)
        } finally {
            mutex.unlock()
        }
    }

    fun recent(count: Int = 50): List<Entry> {
        if (!mutex.tryLock()) return emptyList()
        try {
            return buffer.takeLast(count)
        } finally {
            mutex.unlock()
        }
    }

    fun clear() {
        if (!mutex.tryLock()) return
        try {
            buffer.clear()
        } finally {
            mutex.unlock()
        }
    }
}
