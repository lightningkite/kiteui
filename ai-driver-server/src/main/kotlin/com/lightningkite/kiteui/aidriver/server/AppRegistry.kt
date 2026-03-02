// by Claude - manages all connected app sessions
package com.lightningkite.kiteui.aidriver.server

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object AppRegistry {
    private val sessions = ConcurrentHashMap<String, AppSession>()
    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    fun register(session: AppSession) {
        sessions[session.appId] = session
    }

    fun unregister(appId: String) {
        sessions.remove(appId)
    }

    fun get(appId: String): AppSession? = sessions[appId]

    fun all(): List<AppSession> = sessions.values.toList()

    /** Generate a unique app ID like "android-1", "web-2", etc. */
    fun generateId(platform: String): String {
        val counter = counters.getOrPut(platform) { AtomicInteger(0) }
        return "$platform-${counter.incrementAndGet()}"
    }
}
