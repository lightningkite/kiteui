// by Claude - per-app WebSocket session with serialized request queue
package com.lightningkite.kiteui.aidriver.server

import com.lightningkite.kiteui.aidriver.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicInteger

class AppSession(
    val appId: String,
    val appName: String,
    val platform: String,
    private val session: DefaultWebSocketSession
) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val requestCounter = AtomicInteger(0)
    // by Claude - ConcurrentHashMap for thread safety: onMessage runs on Daemon.scope (Dispatchers.IO)
    // while request methods run on caller coroutines, so concurrent access is possible
    private val pendingRequests = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<AppMessage>>()

    // Flow of snapshots for wait() polling
    private val snapshotFlow = MutableSharedFlow<UiSnapshot>(
        replay = 1,
        extraBufferCapacity = 10
    )

    val snapshots: SharedFlow<UiSnapshot> = snapshotFlow

    // Called when a message arrives from the app
    suspend fun onMessage(msg: AppMessage) {
        when (msg) {
            is AppMessage.SnapshotResponse -> {
                pendingRequests.remove(msg.requestId)?.complete(msg)
                snapshotFlow.emit(msg.snapshot)
            }
            is AppMessage.ScreenshotResponse -> {
                pendingRequests.remove(msg.requestId)?.complete(msg)
            }
            is AppMessage.ActionResult -> {
                pendingRequests.remove(msg.requestId)?.complete(msg)
            }
            is AppMessage.LogsResponse -> {
                pendingRequests.remove(msg.requestId)?.complete(msg)
            }
            is AppMessage.Changed -> {
                // App state changed - request a fresh snapshot to update the flow
                try {
                    val snapshot = requestSnapshot()
                    snapshotFlow.emit(snapshot)
                } catch (e: Exception) {
                    // Ignore snapshot failures on change notifications
                }
            }
            is AppMessage.Register -> { /* handled during connection setup */ }
        }
    }

    suspend fun requestSnapshot(component: String? = null): UiSnapshot {
        val deferred = CompletableDeferred<AppMessage>()
        val requestId = "snap-${requestCounter.incrementAndGet()}"
        pendingRequests[requestId] = deferred

        val payload = json.encodeToString(DaemonMessage.serializer(), DaemonMessage.RequestSnapshot(requestId))
        println("[$appId] Sending RequestSnapshot $requestId: $payload")
        session.send(payload)
        println("[$appId] RequestSnapshot sent, waiting for response...")

        val result = withTimeout(10_000) { deferred.await() }
        println("[$appId] Got response for $requestId: ${result::class.simpleName}")
        return (result as AppMessage.SnapshotResponse).snapshot
    }

    suspend fun performAction(action: UiAction): AppMessage {
        val deferred = CompletableDeferred<AppMessage>()
        val requestId = "act-${requestCounter.incrementAndGet()}"
        pendingRequests[requestId] = deferred

        session.send(json.encodeToString(DaemonMessage.serializer(),
            DaemonMessage.PerformAction(requestId, action)))

        return withTimeout(15_000) { deferred.await() }
    }

    suspend fun requestScreenshot(): AppMessage.ScreenshotResponse {
        val deferred = CompletableDeferred<AppMessage>()
        val requestId = "shot-${requestCounter.incrementAndGet()}"
        pendingRequests[requestId] = deferred

        session.send(json.encodeToString(DaemonMessage.serializer(),
            DaemonMessage.RequestScreenshot(requestId)))

        return withTimeout(15_000) { deferred.await() } as AppMessage.ScreenshotResponse
    }

    // by Claude - request buffered log entries from the connected app
    suspend fun requestLogs(lines: Int = 200): List<LogEntry> {
        val deferred = CompletableDeferred<AppMessage>()
        val requestId = "logs-${requestCounter.incrementAndGet()}"
        pendingRequests[requestId] = deferred
        session.send(json.encodeToString(DaemonMessage.serializer(), DaemonMessage.RequestLogs(requestId, lines)))
        val result = withTimeout(10_000) { deferred.await() }
        return (result as AppMessage.LogsResponse).entries
    }

    // by Claude - send a daemon message and wait for an ActionResult response
    suspend fun sendAndAwait(message: DaemonMessage, requestId: String): AppMessage.ActionResult {
        val deferred = CompletableDeferred<AppMessage>()
        pendingRequests[requestId] = deferred
        session.send(json.encodeToString(DaemonMessage.serializer(), message))
        return withTimeout(15_000) { deferred.await() } as AppMessage.ActionResult
    }

    suspend fun waitFor(
        page: String? = null,
        url: String? = null,
        component: String? = null,
        componentGone: String? = null,
        enabled: String? = null,
        change: Boolean = false,
        timeout: Long = 10_000
    ): UiSnapshot {
        return withTimeout(timeout) {
            snapshotFlow.first { snap ->
                (page == null || snap.page == page) &&
                (url == null || snap.url == url) &&
                (component == null || snap.findComponent(component) != null) &&
                (componentGone == null || snap.findComponent(componentGone) == null) &&
                (enabled == null || snap.findComponent(enabled)?.enabled == true)
            }
        }
    }
}

private fun UiSnapshot.findComponent(path: String): UiComponent? {
    fun List<UiComponent>.find(segments: List<String>): UiComponent? {
        if (segments.isEmpty()) return null
        val head = segments.first()
        val rest = segments.drop(1)
        val match = firstOrNull { it.id.split("/").last() == head || it.id == head }
            ?: return null
        return if (rest.isEmpty()) match else match.children.find(rest)
    }
    return components.find(path.split("/").filter { it.isNotEmpty() })
}
