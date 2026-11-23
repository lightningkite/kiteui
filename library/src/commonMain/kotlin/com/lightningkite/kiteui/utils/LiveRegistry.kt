package com.lightningkite.kiteui.utils

import com.lightningkite.reactive.core.Release
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

public class LiveRegistry<T>(list: List<T> = emptyList()) {
    // Claude: snapshots are taken on any thread (requests on IO, logs anywhere, crash hooks on the crashing thread).
    private val lock = SynchronizedObject()
    private val registry = ArrayList<T>(list)

    public fun register(value: T): Release {
        synchronized(lock) { registry.add(value) }
        return {
            synchronized(lock) {
                val i = registry.indexOf(value)
                if (i != -1) registry.removeAt(i)
            }
        }
    }

    public fun snapshot(): List<T> = synchronized(lock) { registry.toList() }
}
