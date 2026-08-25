package com.lightningkite.kiteui.testserver

import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.contentType
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.utils.io.readRemaining
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.io.readByteArray

/**
 * The HTTP and WebSocket endpoints KiteUI's cross-platform network tests run against.
 *
 * A local server rather than a public one because the tests assert on things no public echo service
 * offers — a chosen status code, a `Retry-After`, a close code, a server that hangs up mid-session —
 * and because a test suite that fails when the office wifi does teaches everyone to ignore it. It
 * also keeps every platform on plain `http`/`ws`, sidestepping the certificate handling that differs
 * between OkHttp, NSURLSession, and a browser and has nothing to do with what these tests measure.
 *
 * Bound to loopback only: it answers anything that asks, so it must not be reachable off the machine.
 */
public fun Application.echoModule(cors: Boolean) {
    install(WebSockets)
    // The browser test page is served by Karma on its own port, so every request from it is
    // cross-origin. Without this the browser refuses them before they reach a route.
    if (cors) install(CORS) {
        anyHost()
        HttpMethod.DefaultMethods.forEach { allowMethod(it) }
        allowHeaders { true }
        allowNonSimpleContentTypes = true
        // A cross-origin page can only read the headers it is told it may; the tests assert on these.
        exposeHeader(HttpHeaders.RetryAfter)
        exposeHeader(ECHO_METHOD_HEADER)
    }
    routing {
        get("/hello") { call.respondText(HELLO_BODY) }

        // Any method, so one route covers GET/POST/PUT/PATCH/DELETE body and method round-trips.
        route("/echo") {
            handle {
                val body = call.receiveChannel().readRemaining().readByteArray()
                call.response.header(ECHO_METHOD_HEADER, call.request.httpMethod.value)
                call.respondBytes(body, call.request.contentType())
            }
        }

        // One "name: value" per line, sorted, so a test can assert the headers it asked for arrived.
        get("/headers") {
            call.respondText(
                call.request.headers.entries()
                    .flatMap { entry -> entry.value.map { "${entry.key.lowercase()}: $it" } }
                    .sorted()
                    .joinToString("\n")
            )
        }

        // The status codes ConnectivityGate treats specially (429, 502, 503) plus ordinary failures.
        get("/status/{code}") {
            val code = call.parameters["code"]!!.toInt()
            if (code == HttpStatusCode.TooManyRequests.value) {
                call.response.header(HttpHeaders.RetryAfter, RETRY_AFTER_SECONDS.toString())
            }
            call.respondText(
                text = "status $code",
                status = HttpStatusCode.fromValue(code),
            )
        }

        // A known-length body, which is what makes a download-progress total meaningful.
        get("/bytes/{count}") {
            val count = call.parameters["count"]!!.toInt()
            call.response.header(HttpHeaders.CacheControl, CacheControl.NoCache(null).toString())
            call.respondBytes(deterministicBytes(count), ContentType.Application.OctetStream)
        }

        // Echoes text and binary frames unchanged.
        webSocket("/ws/echo") {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> send(Frame.Text(frame.readText()))
                    is Frame.Binary -> send(Frame.Binary(true, frame.data))
                    else -> {}
                }
            }
        }

        // Closes with the requested code the moment the socket opens.
        webSocket("/ws/close") {
            val code = call.request.queryParameters["code"]?.toShort() ?: CloseReason.Codes.NORMAL.code
            close(CloseReason(code, call.request.queryParameters["reason"] ?: ""))
        }

        // Announces which connection this is, then hangs up on request. Between them a client can
        // tell an established connection from a re-established one, which is what a reconnect is.
        webSocket("/ws/session") {
            send(Frame.Text("$SESSION_PREFIX${sessions.incrementAndGet()}"))
            for (frame in incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                if (text == DROP_COMMAND) {
                    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "dropped on request"))
                    return@webSocket
                }
                send(Frame.Text(text))
            }
        }
    }
}

public const val HELLO_BODY: String = "hello, kiteui"
public const val ECHO_METHOD_HEADER: String = "X-Echo-Method"
public const val RETRY_AFTER_SECONDS: Int = 7
public const val SESSION_PREFIX: String = "session "
public const val DROP_COMMAND: String = "drop"

/** Content a test can predict byte for byte without the server and the test sharing a buffer. */
public fun deterministicBytes(count: Int): ByteArray = ByteArray(count) { (it % 251).toByte() }

private val sessions = AtomicInteger(0)

/** Starts the server on [port]; the caller stops the returned engine. */
public fun startEchoServer(port: Int, cors: Boolean): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> =
    embeddedServer(Netty, port = port, host = LOOPBACK, module = { echoModule(cors) }).apply { start(wait = false) }

public const val LOOPBACK: String = "127.0.0.1"
