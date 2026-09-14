package com.lightningkite.kiteui

/**
 * A thread-safe, fixed-capacity ring buffer. When full, the oldest entry is dropped.
 *
 * Synchronization is platform-native: `synchronized` on JVM/Android,
 * `NSLock` on iOS, no-op on JS (single-threaded).
 */
internal class RingBuffer<T>(private val maxSize: Int) {
    private val buffer = ArrayDeque<T>()

    fun add(item: T): Unit = platformSynchronized(this) {
        if (buffer.size >= maxSize) buffer.removeFirst()
        buffer.addLast(item)
    }

    fun takeLast(count: Int): List<T> = platformSynchronized(this) {
        buffer.takeLast(count)
    }

    fun clear(): Unit = platformSynchronized(this) {
        buffer.clear()
    }
}

/**
 * Platform-native mutual exclusion.
 * - JVM/Android: delegates to [kotlin.synchronized] (Java monitor lock).
 * - JS: no-op (single-threaded).
 * - iOS: `NSLock`.
 */
internal expect fun <T> platformSynchronized(lock: Any, block: () -> T): T
