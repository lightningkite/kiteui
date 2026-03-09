// by Claude - per-app WebSocket session with serialized request queue
package com.lightningkite.kiteui.aidriver.server

import com.lightningkite.kiteui.aidriver.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.drop
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
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e // by Claude - don't swallow CancellationException
                } catch (e: Exception) {
                    // Ignore snapshot failures on change notifications
                }
            }
            is AppMessage.Register -> { /* handled during connection setup */ }
        }
    }

    // by Claude - helper to register a pending request, send a message, and await with timeout + cleanup
    private suspend fun <T> sendAndAwaitInternal(
        requestId: String,
        message: DaemonMessage,
        timeoutMs: Long,
        transform: (AppMessage) -> T
    ): T {
        val deferred = CompletableDeferred<AppMessage>()
        pendingRequests[requestId] = deferred
        try {
            session.send(json.encodeToString(DaemonMessage.serializer(), message))
            val result = withTimeout(timeoutMs) { deferred.await() }
            return transform(result)
        } finally {
            pendingRequests.remove(requestId)
        }
    }

    suspend fun requestSnapshot(component: String? = null): UiSnapshot {
        val requestId = "snap-${requestCounter.incrementAndGet()}"
        return sendAndAwaitInternal(requestId, DaemonMessage.RequestSnapshot(requestId), 10_000) {
            (it as AppMessage.SnapshotResponse).snapshot
        }
    }

    suspend fun performAction(action: UiAction): AppMessage {
        val requestId = "act-${requestCounter.incrementAndGet()}"
        return sendAndAwaitInternal(requestId, DaemonMessage.PerformAction(requestId, action), 15_000) { it }
    }

    suspend fun requestScreenshot(): AppMessage.ScreenshotResponse {
        val requestId = "shot-${requestCounter.incrementAndGet()}"
        return sendAndAwaitInternal(requestId, DaemonMessage.RequestScreenshot(requestId), 15_000) {
            it as AppMessage.ScreenshotResponse
        }
    }

    // by Claude - request buffered log entries from the connected app
    suspend fun requestLogs(lines: Int = 200): List<LogEntry> {
        val requestId = "logs-${requestCounter.incrementAndGet()}"
        return sendAndAwaitInternal(requestId, DaemonMessage.RequestLogs(requestId, lines), 10_000) {
            (it as AppMessage.LogsResponse).entries
        }
    }

    // by Claude - send a daemon message and wait for an ActionResult response
    suspend fun sendAndAwait(message: DaemonMessage, requestId: String): AppMessage.ActionResult =
        sendAndAwaitInternal(requestId, message, 15_000) { it as AppMessage.ActionResult }

    // by Claude - complete all pending requests exceptionally when the app disconnects
    fun cancelPending() {
        val ex = java.util.concurrent.CancellationException("App $appId disconnected")
        pendingRequests.forEach { (_, deferred) -> deferred.completeExceptionally(ex) }
        pendingRequests.clear()
    }

    // by Claude - change=true skips the first (replayed) snapshot so we wait for an actual state change
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
            val flow = if (change) snapshotFlow.drop(1) else snapshotFlow
            flow.first { snap ->
                (page == null || snap.page == page) &&
                (url == null || snap.url == url) &&
                (component == null || snap.findComponent(component) != null) &&
                (componentGone == null || snap.findComponent(componentGone) == null) &&
                (enabled == null || snap.findComponent(enabled)?.enabled == true)
            }
        }
    }
}

// by Claude - delegates to UiSnapshot.findById which supports relative IDs from compact format
private fun UiSnapshot.findComponent(path: String): UiComponent? = findById(path)
