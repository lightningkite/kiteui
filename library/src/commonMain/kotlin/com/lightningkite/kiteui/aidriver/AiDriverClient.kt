// by Claude - AI driver client: connects a KiteUI app outbound to the AI driver daemon
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.*
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * AI driver client — connects a KiteUI app outbound to the AI driver daemon.
 *
 * Call [connect] once during app startup (debug/QA builds only) to enable LLM-driven
 * UI automation. The daemon runs on the developer's machine and drives the app via
 * WebSocket commands; the app responds with UI snapshots and action results.
 *
 * Uses KiteUI's built-in [retryWebsocket] for automatic reconnection — no Ktor
 * client setup required in the app.
 */
object AiDriver {
    internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var socket: TypedWebSocket<AppMessage, DaemonMessage>? = null

    /**
     * Start connecting to the AI driver daemon.
     * Safe to call multiple times — only one connection is maintained.
     * Uses [retryWebsocket] for automatic reconnection when the daemon isn't running.
     *
     * @param appName           Human-readable name reported to the daemon on registration.
     * @param host              Daemon hostname (default: "localhost").
     * @param port              Daemon WebSocket port (default: 7474).
     * @param rootViewProvider  Returns the root [RView] to snapshot. May be null while loading.
     * @param navigatorProvider Returns the primary [PageNavigator] for navigation actions.
     */
    fun connect(
        appName: String,
        host: String = "localhost",
        port: Int = 7474,
        rootViewProvider: () -> RView?,
        navigatorProvider: () -> PageNavigator?
    ) {
        if (socket != null) return
        val platform = Platform.current.name.lowercase()

        // by Claude — use a separate gate so AiDriver connection failures don't block app network
        // (the daemon may not be running, and that shouldn't affect real API calls)
        val aiDriverGate = ConnectivityGate()
        val raw = retryWebsocket("ws://$host:$port/app", pingTime = 30_000L, gate = aiDriverGate)
        val typed = raw.typed(json, AppMessage.serializer(), DaemonMessage.serializer())

        // Send Register and hook into navigator on every successful connection
        var navCleanup: (() -> Unit)? = null
        typed.onOpen {
            typed.send(AppMessage.Register(appName, platform))
            navCleanup?.invoke()
            // Watch the page navigator stack and notify the daemon on navigation
            navCleanup = navigatorProvider()?.stack?.addListener {
                typed.send(AppMessage.Changed)
            }
        }
        typed.onClose {
            navCleanup?.invoke()
            navCleanup = null
        }

        // Handle incoming daemon commands on the app's main scope
        typed.onMessage { msg ->
            AppScope.launch {
                handleMessage(msg, typed, rootViewProvider, navigatorProvider)
            }
        }

        raw.beginUse()
        socket = typed
    }

    /** Close the connection and stop reconnecting. */
    fun disconnect() {
        socket?.close(1000, "Disconnected")
        socket = null
    }

    // by Claude - lazily installed MockExternalServices for AI driver mock commands
    private var mockServices: MockExternalServices? = null

    /** Ensure a MockExternalServices is installed on the root view's context, wrapping any existing one. */
    private fun ensureMockInstalled(rootViewProvider: () -> RView?): MockExternalServices {
        mockServices?.let { return it }
        val root = rootViewProvider()
        val existing = root?.context?.addons?.get("externalServices") as? ExternalServicesAccess
        val mock = MockExternalServices(delegate = existing)
        root?.context?.addons?.set("externalServices", mock)
        mockServices = mock
        return mock
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun handleMessage(
        msg: DaemonMessage,
        socket: TypedWebSocket<AppMessage, DaemonMessage>,
        rootViewProvider: () -> RView?,
        navigatorProvider: () -> PageNavigator?
    ) {
        when (msg) {
            is DaemonMessage.RequestSnapshot -> {
                val root = rootViewProvider()
                if (root == null) {
                    socket.send(AppMessage.ActionResult(msg.requestId, error = "No root view available"))
                    return
                }
                val snapshot = buildSnapshot(root, navigatorProvider())
                socket.send(AppMessage.SnapshotResponse(msg.requestId, snapshot))
            }
            // by Claude - always send ScreenshotResponse (not ActionResult) for screenshot requests,
            // because the server casts the response to ScreenshotResponse
            is DaemonMessage.RequestScreenshot -> {
                val root = rootViewProvider()
                if (root == null) {
                    socket.send(AppMessage.ScreenshotResponse(msg.requestId, error = "No root view available"))
                    return
                }
                val result = dispatchAction(UiAction.Screenshot, root, navigatorProvider())
                socket.send(AppMessage.ScreenshotResponse(
                    requestId = msg.requestId,
                    error = result.error,
                    base64 = result.bytes?.let { Base64.Default.encode(it) }
                ))
            }
            is DaemonMessage.PerformAction -> {
                val root = rootViewProvider()
                if (root == null) {
                    socket.send(AppMessage.ActionResult(msg.requestId, error = "No root view available"))
                    return
                }
                val result = dispatchAction(msg.action, root, navigatorProvider())
                socket.send(result.toAppMessage(msg.requestId))
            }
            // by Claude - respond with buffered log entries
            is DaemonMessage.RequestLogs -> {
                val entries = AiDriverLogBuffer.entries(msg.lines)
                socket.send(AppMessage.LogsResponse(msg.requestId, entries))
            }
            // by Claude - handle mock injection from daemon
            is DaemonMessage.QueueMockFile -> {
                try {
                    val mock = ensureMockInstalled(rootViewProvider)
                    val bytes = Base64.Default.decode(msg.base64)
                    val fileRef = createFileReferenceFromBytes(bytes, msg.mimeType, msg.fileName)
                    mock.pendingFileResponses.add(fileRef)
                    socket.send(AppMessage.ActionResult(msg.requestId))
                } catch (e: Exception) {
                    socket.send(AppMessage.ActionResult(msg.requestId, error = "Failed to queue mock file: ${e.message}"))
                }
            }
            is DaemonMessage.QueueMockGeolocation -> {
                try {
                    val mock = ensureMockInstalled(rootViewProvider)
                    mock.pendingGeolocation.add(GeolocationResult(
                        latitude = msg.latitude,
                        longitude = msg.longitude,
                        accuracyInMeters = msg.accuracyInMeters
                    ))
                    socket.send(AppMessage.ActionResult(msg.requestId))
                } catch (e: Exception) {
                    socket.send(AppMessage.ActionResult(msg.requestId, error = "Failed to queue mock geolocation: ${e.message}"))
                }
            }
        }
    }
}

// by Claude - carries the result of a dispatched UI action back to the socket layer
data class ActionDispatchResult(
    val success: Boolean,
    val error: String? = null,
    val bytes: ByteArray? = null
) {
    @OptIn(ExperimentalEncodingApi::class)
    fun toAppMessage(requestId: String): AppMessage =
        if (bytes != null) AppMessage.ScreenshotResponse(requestId, error, Base64.Default.encode(bytes))
        else AppMessage.ActionResult(requestId, error)
}
