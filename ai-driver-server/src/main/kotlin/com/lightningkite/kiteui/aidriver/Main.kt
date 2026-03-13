package com.lightningkite.kiteui.aidriver

import com.lightningkite.lightningserver.auth.noAuth
import com.lightningkite.lightningserver.definition.*
import com.lightningkite.lightningserver.serialization.registerBasicMediaTypeCoders
import com.lightningkite.lightningserver.definition.builder.ServerBuilder
import com.lightningkite.lightningserver.engine.ktor.KtorEngine
import com.lightningkite.lightningserver.engine.ktor.KtorRuntimeSettings
import com.lightningkite.lightningserver.engine.ktor.ktorRunConfig
import com.lightningkite.lightningserver.engine.local.engineCache
import com.lightningkite.lightningserver.engine.local.enginePubSub
import com.lightningkite.lightningserver.engine.local.forceWebSocketPubSub
import com.lightningkite.lightningserver.http.*
import com.lightningkite.lightningserver.pathing.PathSpec0
import com.lightningkite.lightningserver.plainText
import com.lightningkite.lightningserver.runtime.ServerRuntime
import com.lightningkite.lightningserver.settings.set
import com.lightningkite.lightningserver.typed.jsonrpc.JsonRpcHandler
import com.lightningkite.lightningserver.typed.jsonrpc.JsonRpcMethod
import com.lightningkite.lightningserver.websockets.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

/**
 * AI Driver relay server built on Lightning Server.
 * - Apps connect via WebSocket at ws://localhost:7474
 * - CLI / tests send commands via HTTP POST to http://localhost:7474
 * - MCP clients connect via JSON-RPC at http://localhost:7474/mcp
 */

class AppConnection(
    val id: String,
    val send: suspend (WebSocketFrame) -> Unit,
    var pending: CompletableDeferred<String>? = null,
) {
    val mutex = Mutex()
    suspend fun sendCommand(command: String): String {
        val deferred = CompletableDeferred<String>()
        // Hold the mutex for the entire send+await cycle so concurrent callers
        // are serialized and cannot overwrite `pending` before we receive our response.
        return mutex.withLock {
            pending = deferred
            send(WebSocketFrame.Text(command))
            withTimeout(30_000) { deferred.await() }
        }
    }
}

val apps = ConcurrentHashMap<String, AppConnection>()

/** Global mutex for the drive tool — ensures one command chain at a time. */
private val driveMutex = Mutex()

/** Resolve an app by exact ID or prefix match (most recent). */
internal fun resolveApp(appId: String): AppConnection? =
    apps[appId] ?: apps.entries
        .filter { it.key.startsWith(appId) }
        .maxByOrNull { it.key }
        ?.value

// ===================== CLI business logic =====================

/** Process a CLI command (HTTP POST body). Returns the response text. */
internal suspend fun processCliCommand(body: String): String {
    val trimmed = body.trim()
    if (trimmed.isBlank()) return "ERROR: empty command"

    if (trimmed == "ls") {
        return apps.keys.sorted().joinToString("\n").ifEmpty { "(no apps connected)" }
    }

    val splitIdx = trimmed.indexOf('\t')
    val appId = if (splitIdx >= 0) trimmed.substring(0, splitIdx) else trimmed
    val cmd = if (splitIdx >= 0) trimmed.substring(splitIdx + 1) else null
    if (cmd == null) return "ERROR: no command after app ID"

    val conn = resolveApp(appId)
        ?: return "ERROR: app '$appId' not found. Connected: ${apps.keys.sorted()}"

    return try {
        conn.sendCommand(cmd)
    } catch (e: Exception) {
        "ERROR: ${e.message}"
    }
}

// ===================== MCP tool logic =====================

internal fun executeLs(): String =
    apps.keys.sorted().joinToString("\n").ifEmpty { "(no apps connected)" }

private val screenshotDir = File(System.getProperty("java.io.tmpdir"), "kiteui-screenshots").also { it.mkdirs() }
private val screenshotCounter = AtomicInteger(0)

@OptIn(ExperimentalEncodingApi::class)
internal suspend fun executeDrive(args: McpDriveParams): McpToolResult {
    val conn = resolveApp(args.app)
        ?: return McpToolResult(
            content = listOf(McpContentItem(text = "ERROR: app '${args.app}' not found. Connected: ${apps.keys.sorted()}")),
            isError = true
        )

    val contentItems = mutableListOf<McpContentItem>()
    var hasError = false
    driveMutex.withLock {
        for (parts in args.commands) {
            if (parts.firstOrNull() == "delay") {
                val ms = (parts.getOrNull(1)?.toLongOrNull() ?: 0L).coerceIn(0, 30_000)
                delay(ms)
                continue
            }
            val action = parts.getOrNull(1)
            val cmd = parts.joinToString("\t")
            try {
                val response = conn.sendCommand(cmd)
                if (response.startsWith("ERROR:")) {
                    contentItems.add(McpContentItem(text = response))
                    hasError = true
                    break
                }
                if (action == "screenshot") {
                    val file = File(screenshotDir, "screenshot-${screenshotCounter.incrementAndGet()}.png")
                    file.writeBytes(Base64.decode(response))
                    contentItems.add(McpContentItem(text = "Screenshot saved: ${file.absolutePath}"))
                } else {
                    contentItems.add(McpContentItem(text = response))
                }
            } catch (e: Exception) {
                contentItems.add(McpContentItem(text = "ERROR: ${e.message}"))
                hasError = true
                break
            }
        }
    }
    return McpToolResult(content = contentItems, isError = hasError)
}

// ===================== MCP data models =====================

@Serializable
data class McpInitializeParams(
    val protocolVersion: String = "2025-03-26",
    val capabilities: JsonObject = JsonObject(emptyMap()),
    val clientInfo: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String,
    val capabilities: JsonObject,
    val serverInfo: JsonObject,
    val instructions: String,
)

@Serializable
data class McpToolsListParams(val cursor: String? = null)

@Serializable
data class McpToolsListResult(val tools: List<JsonObject>)

@Serializable
data class McpToolsCallParams(
    val name: String,
    val arguments: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class McpDriveParams(
    val app: String,
    val commands: List<List<String>>,
)

@Serializable
data class McpToolResult(
    val content: List<McpContentItem>,
    val isError: Boolean = false,
)

@Serializable
data class McpContentItem(
    val type: String = "text",
    val text: String? = null,
)

// ===================== MCP instructions & tool definitions =====================

internal val MCP_INSTRUCTIONS = """
KiteUI AI Driver — command format:

Commands are sent as arrays of strings: [viewPath, action, ...args]

Actions:
  snapshot [--interactive] [--hidden] [--themes]  — tree of view hierarchy
  screenshot                                       — base64 PNG
  find <text>                                      — find view by text content
  findClickable <text>                             — find clickable ancestors of matching views
  click                                            — click/tap a view
  longClick                                        — long press a view
  setValue <value>                                  — set text input value
  toggle                                           — toggle checkbox/switch
  select <option>                                   — select dropdown option
  submit                                            — submit a form
  scrollIntoView                                    — scroll view into visible area
  getDragData                                       — get drag data from a view
  drop <base64data>                                 — drop drag data on a view

Special commands (no viewPath):
  navigate <route>       — navigate to URL-like route
  url                    — get current page URL
  back back              — go back
  logs <count>           — get recent log entries
  mock file <base64> <mime> <name>  — queue mock file
  mock fileNull          — queue null file response
  mock geolocation <lat> <lon> [accuracy]
  mock calls             — view recorded call log
  mock clearCalls        — clear recorded call log

Virtual commands (MCP only):
  delay <ms>             — pause between commands (e.g. ["delay", "500"])

Example chain: [["root", "snapshot", "--interactive"], ["root/loginButton", "click"], ["delay", "500"], ["root", "snapshot"]]
""".trimIndent()

private val TOOL_LS = buildJsonObject {
    put("name", "ls")
    put("description", "List connected KiteUI apps")
    put("inputSchema", buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {})
    })
}

private val TOOL_DRIVE = buildJsonObject {
    put("name", "drive")
    put(
        "description",
        "Send commands to a connected KiteUI app. Commands execute sequentially. Use 'delay <ms>' to pause between commands. Stops on first error (response starting with 'ERROR:')."
    )
    put("inputSchema", buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("app", buildJsonObject {
                put("type", "string")
                put("description", "App ID (exact or prefix match)")
            })
            put("commands", buildJsonObject {
                put("type", "array")
                put("items", buildJsonObject {
                    put("type", "array")
                    put("items", buildJsonObject { put("type", "string") })
                    put("description", "Command as [viewPath, action, ...args]. Special: [\"delay\", \"ms\"], [\"navigate\", \"route\"], [\"url\"], [\"back\"], [\"logs\", \"count\"].")
                })
                put("description", "List of commands. Each command is an array of strings: [viewPath, action, ...args].")
            })
        })
        putJsonArray("required") { add("app"); add("commands") }
    })
}

// ===================== MCP JSON-RPC methods =====================

private val initializeMethod = JsonRpcMethod<PathSpec0, Nothing?, McpInitializeParams, McpInitializeResult>(
    name = "initialize",
    auth = noAuth,
    implementation = {
        McpInitializeResult(
            protocolVersion = "2025-03-26",
            capabilities = buildJsonObject { put("tools", buildJsonObject {}) },
            serverInfo = buildJsonObject { put("name", "kiteui-driver"); put("version", "1.0.0") },
            instructions = MCP_INSTRUCTIONS
        )
    }
)

private val toolsListMethod = JsonRpcMethod<PathSpec0, Nothing?, McpToolsListParams?, McpToolsListResult>(
    name = "tools/list",
    auth = noAuth,
    implementation = { McpToolsListResult(tools = listOf(TOOL_LS, TOOL_DRIVE)) }
)

private val toolsCallMethod = JsonRpcMethod<PathSpec0, Nothing?, McpToolsCallParams, McpToolResult>(
    name = "tools/call",
    auth = noAuth,
    implementation = { params ->
        when (params.name) {
            "ls" -> McpToolResult(content = listOf(McpContentItem(text = executeLs())))
            "drive" -> {
                val driveParams = Json.decodeFromJsonElement<McpDriveParams>(params.arguments)
                executeDrive(driveParams)
            }
            else -> McpToolResult(
                content = listOf(McpContentItem(text = "ERROR: unknown tool '${params.name}'")),
                isError = true
            )
        }
    }
)

// ===================== Lightning Server wiring =====================

object AiDriverServer : ServerBuilder() {
    init {
        registerBasicMediaTypeCoders()
        path bind AppWebSocketHandler
        path.post bind HttpHandler { request ->
            HttpResponse.plainText(processCliCommand(request.body?.text() ?: ""))
        }
        path.path("mcp").post bind JsonRpcHandler(
            methods = listOf(initializeMethod, toolsListMethod, toolsCallMethod)
        )
    }
}

internal object AppWebSocketHandler : WebSocketHandler<PathSpec0, Unit>,
    DirectExecutableWebSocketHandler<PathSpec0> {
    override val storageSerializer: KSerializer<Unit> = Unit.serializer()

    override suspend fun handleDirect(
        serverRuntime: ServerRuntime,
        request: WebSocketConnectRequest<PathSpec0>,
        incoming: ReceiveChannel<WebSocketFrame>,
        send: suspend (WebSocketFrame) -> Unit,
        close: suspend (WebSocketClose) -> Unit
    ) {
        // Identity is self-declared via query params. This is safe because the server
        // binds to 127.0.0.1 only — no remote access. The random 3-letter postfix in
        // AiDriver.connect() avoids accidental collisions between app instances.
        val params = request.queryParameters
        val appName = params["app"] ?: "unknown"
        val platform = params["platform"] ?: "unknown"
        val postfix = params["postfix"] ?: "x"
        val id = "$appName-$platform-$postfix"
        apps[id]?.let { old ->
            try {
                old.send(WebSocketFrame.Text(""))
            } catch (_: Exception) {
            }
        }

        val conn = AppConnection(id, send)
        apps[id] = conn
        println("App connected: $id")

        try {
            incoming.consumeEach { frame ->
                if (frame is WebSocketFrame.Text && frame.content.isNotBlank()) {
                    conn.mutex.withLock {
                        conn.pending?.complete(frame.content)
                        conn.pending = null
                    }
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

    // Standard WebSocketHandler methods — unused because DirectExecutableWebSocketHandler takes priority
    context(_: ServerRuntime)
    override suspend fun willConnect(request: WebSocketConnectRequest<PathSpec0>) = Unit

    context(_: WebSocketConnection<PathSpec0, Unit>)
    override suspend fun didConnect() {
    }

    context(_: WebSocketConnection<PathSpec0, Unit>)
    override suspend fun messageFromClient(frame: WebSocketFrame) {
    }

    context(_: WebSocketConnection<PathSpec0, Unit>)
    override suspend fun messageFromSubscription(topic: WebSocketSubscriptionMessage<*, *>) {
    }

    context(_: WebSocketConnection<PathSpec0, Unit>)
    override suspend fun disconnect(reason: WebSocketClose) {
    }
}

// ===================== ADB Reverse =====================

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
                    .drop(1)
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
                knownDevices.keys.removeAll { it !in serials }
            } catch (_: Exception) {
                return@launch
            }
            delay(5000)
        }
    }
}

// ===================== Entry Point =====================

fun main(args: Array<String>) {
    val port = args.firstOrNull()?.toIntOrNull() ?: 7474
    println("KiteUI AI Driver server starting on port $port")
    startAdbReverse(port)

    val definition = AiDriverServer.build()
    KtorEngine(definition).apply {
        settings.run {
            generalSettings.useDefault()
            secretBasis.useDefault()
            telemetrySettings.useDefault()
            loggingSettings.useDefault()
            enginePubSub.useDefault()
            engineCache.useDefault()
            forceWebSocketPubSub.useDefault()
            ktorRunConfig set KtorRuntimeSettings(host = "127.0.0.1", port = port)
        }
        start(Netty)
    }
}
