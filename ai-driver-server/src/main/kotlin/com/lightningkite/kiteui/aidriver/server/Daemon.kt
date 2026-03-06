// by Claude - Ktor daemon: WebSocket server (:7474) + CLI HTTP server (:7475)
package com.lightningkite.kiteui.aidriver.server

import com.lightningkite.kiteui.aidriver.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

object Daemon {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // Coroutine scope for background work spawned by session message handlers
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start(port: Int = 7474, cliPort: Int = 7475, daemon: Boolean = false) {
        println("Starting AI driver daemon on ws://localhost:$port (CLI on http://localhost:$cliPort)")

        // by Claude - bind to localhost only; the daemon is a local dev tool and should
        // never be exposed on the network (it can drive UI and inject mocks)
        val appServer = embeddedServer(Netty, port = port, host = "127.0.0.1") {
            install(WebSockets) {
                // by Claude - keep connections alive; KiteUI retryWebsocket pings every 30s via text frame
                pingPeriod = 20.seconds
                timeout = 60.seconds
            }
            routing {
                webSocket("/app") {
                    handleAppConnection(this)
                }
            }
        }

        // HTTP server for CLI commands (localhost only)
        val cliServer = embeddedServer(Netty, port = cliPort, host = "127.0.0.1") {
            routing {
                cliRoutes()
            }
        }

        appServer.start(wait = false)
        cliServer.start(wait = false)

        // by Claude - auto-setup adb reverse for Android devices
        AdbReverse.start(scope, port)

        println("AI driver daemon started. Run 'ui list' to see connected apps.")

        Runtime.getRuntime().addShutdownHook(Thread {
            AdbReverse.stop()
            appServer.stop(1000, 5000)
            cliServer.stop(1000, 5000)
        })

        if (!daemon) {
            // Block the main thread so the process stays alive
            Thread.currentThread().join()
        }
    }

    private suspend fun handleAppConnection(session: DefaultWebSocketSession) {
        // Read individual frames via receive() to avoid the for-loop+break pattern,
        // which would cancel the incoming channel and close the WebSocket.
        // by Claude - skip keepalive pings (single space) before Register arrives
        var firstText: String? = null
        while (firstText == null) {
            val frame = try { session.incoming.receive() } catch (_: Exception) { return }
            if (frame is Frame.Text) {
                val text = frame.readText()
                if (text.isNotBlank()) firstText = text
            }
        }

        val firstMsg = try {
            json.decodeFromString(AppMessage.serializer(), firstText)
        } catch (e: Exception) {
            session.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Invalid message: ${e.message}"))
            return
        }

        if (firstMsg !is AppMessage.Register) {
            session.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Expected Register message"))
            return
        }

        val appId = AppRegistry.generateId(firstMsg.platform)
        val appSession = AppSession(appId, firstMsg.appName, firstMsg.platform, session)
        AppRegistry.register(appSession)
        println("App connected: $appId (${firstMsg.appName} on ${firstMsg.platform})")

        try {
            for (frame in session.incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    // by Claude - skip KiteUI retryWebsocket keepalive pings (sent as a single space)
                    if (text.isBlank()) continue
                    try {
                        val msg = json.decodeFromString(AppMessage.serializer(), text)
                        // onMessage may launch its own snapshot request (for Changed),
                        // so run it in the shared scope to avoid blocking the receive loop.
                        scope.launch { appSession.onMessage(msg) }
                    } catch (e: Exception) {
                        println("Error processing message from $appId: ${e.message}")
                        println("JSON input: $text")
                    }
                }
            }
        } finally {
            AppRegistry.unregister(appId)
            println("App disconnected: $appId")
        }
    }
}
