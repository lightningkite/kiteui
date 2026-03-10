package com.lightningkite.kiteui.aidriver

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.websocket.Frame
import io.ktor.websocket.Frame.Text
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * AI Driver relay server.
 * - Apps connect via WebSocket at ws://localhost:7474?app=<name>&platform=<platform>
 * - CLI / tests send commands via HTTP POST to http://localhost:7474
 * - The server forwards the text command to the app's WebSocket and returns the text response.
 */

class AppConnection(
    val id: String,
    val session: DefaultWebSocketServerSession,
    var pending: CompletableDeferred<String>? = null,
) {
    val mutex = Mutex()
}

val apps = ConcurrentHashMap<String, AppConnection>()

fun startAdbReverse(port: Int) {
    val knownDevices = ConcurrentHashMap<String, Boolean>()
    CoroutineScope(Dispatchers.IO).launch {
        while (true) {
            try {
                val process = ProcessBuilder("adb", "devices")
                    .redirectErrorStream(true)
                    .start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                val serials = output.lines()
                    .drop(1) // Skip "List of devices attached" header
                    .filter { it.contains("\tdevice") }
                    .map { it.split("\t").first() }
                for (serial in serials) {
                    if (knownDevices.putIfAbsent(serial, true) == null) {
                        try {
                            ProcessBuilder("adb", "-s", serial, "reverse", "tcp:$port", "tcp:$port")
                                .redirectErrorStream(true)
                                .start()
                                .waitFor()
                            println("ADB reverse set for device $serial on port $port")
                        } catch (e: Exception) {
                            println("ADB reverse failed for $serial: ${e.message}")
                        }
                    }
                }
                // Remove disconnected devices
                knownDevices.keys.removeAll { it !in serials }
            } catch (_: Exception) {
                // adb not on PATH or not installed — silently stop polling
                return@launch
            }
            delay(5000)
        }
    }
}

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 7474
    println("KiteUI AI Driver server starting on port $port")
    startAdbReverse(port)

    embeddedServer(Netty, port = port, host = "127.0.0.1") {
        install(WebSockets)
        routing {
            configureRouting()
        }
    }.start(wait = true)
}

fun Routing.configureRouting() {
    webSocket("/") {
        val params = call.request.queryParameters
        val appName = params["app"] ?: "unknown"
        val platform = params["platform"] ?: "unknown"
        val postfix = params["postfix"] ?: "x"
        val base = "$appName-$platform"
        val id = "$base-${postfix}"
        apps[id]?.session?.close()

        val conn = AppConnection(id, this)
        apps[id] = conn
        println("App connected: $id")

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        if (text.isBlank()) continue // ping
                        conn.mutex.withLock {
                            conn.pending?.complete(text)
                            conn.pending = null
                        }
                    }
                    else -> {}
                }
            }
        } finally {
            apps.remove(id, conn)
            conn.mutex.withLock {
                conn.pending?.complete("ERROR: App disconnected")
            }
            println("App disconnected: $id")
        }
    }

    post("/") {
        val body = call.receiveText().trim()
        if (body.isBlank()) {
            call.respondText("ERROR: empty command")
            return@post
        }

        // Special command: ls
        if (body == "ls") {
            val listing = apps.keys.sorted().joinToString("\n").ifEmpty { "(no apps connected)" }
            call.respondText(listing)
            return@post
        }

        // Parse: first tab-separated field is app ID, rest is forwarded to the app.
        val splitIdx = body.indexOf('\t')
        val appId = if (splitIdx >= 0) body.substring(0, splitIdx) else body
        val command = if (splitIdx >= 0) body.substring(splitIdx + 1) else null
        if (command == null) {
            call.respondText("ERROR: no command after app ID")
            return@post
        }

        // Resolve app — exact match or prefix match (most recent)
        val conn = apps[appId] ?: apps.entries
            .filter { it.key.startsWith(appId) }
            .maxByOrNull { it.key }
            ?.value

        if (conn == null) {
            call.respondText("ERROR: app '$appId' not found. Connected: ${apps.keys.sorted()}")
            return@post
        }

        // Send command and await response — timeout covers both mutex acquisition and response wait
        try {
            val response = withTimeout(30_000) {
                conn.mutex.withLock {
                    val deferred = CompletableDeferred<String>()
                    conn.pending = deferred
                    conn.session.send(Text(command))
                    deferred.await()
                }
            }
            call.respondText(response)
        } catch (e: Exception) {
            call.respondText("ERROR: ${e.message}")
        }
    }
}

