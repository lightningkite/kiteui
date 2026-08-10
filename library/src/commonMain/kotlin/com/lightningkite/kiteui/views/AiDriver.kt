package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kotlinx.serialization.uri.encodeURIComponent
import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.launch
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * AI Driver client — connects to a relay server via WebSocket and handles text commands.
 * Each command is a plain string; the response is a plain string sent back on the same socket.
 */
public object AiDriver {
    private val log = LogRoot.tag("AiDriver")

    public typealias Actions = Map<String, suspend (List<String>) -> String>

    public fun connect(
        appName: String,
        platform: String = Platform.current.name.lowercase(),
        postfix: String? = buildString {
            append(('A'..'Z').random())
            append(('A'..'Z').random())
            append(('A'..'Z').random())
        },
        host: String = "localhost",
        port: Int = 7474,
        rootView: () -> Element?,
        navigator: () -> PageNavigator?,
    ) {
        tryAutoStartDaemon(port)

        // Install log interceptor that buffers for the driver's "logs" command
        if (Log.interceptors.none { it is BufferingLogInterceptor }) {
            Log.interceptors.add(BufferingLogInterceptor())
        }

        val gate = ConnectivityGate()
        val ws = retryWebsocket(
            url = "ws://$host:$port?app=${encodeURIComponent(appName)}&platform=${encodeURIComponent(platform)}&postfix=${encodeURIComponent(postfix ?: "x")}",
            pingTime = 30_000,
            gate = gate,
        )

        ws.onOpen {
            log.info("Connected to driver server at $host:$port as $appName-$platform-$postfix")
        }
        ws.onClose {
            log.info("Driver server disconnected")
        }

        ws.onMessage { message ->
            AppScope.launch {
                try {
                    log.info("Got command $message")
                    val response = handleCommand(message.trim(), rootView(), navigator())
                    ws.send(response)
                } catch (e: Exception) {
                    try {
                        ws.send("ERROR: ${e::class.simpleName}: ${e.message}")
                    } catch (_: Exception) {
                        // WebSocket may already be disconnected; nothing we can do.
                    }
                }
            }
        }

        // Activate the WebSocket — retryWebsocket uses lazy connection via beginUse()
        ws.beginUse()
    }

    public object Defaults {
        public fun defaultDriverActions(element: Element): AiDriver.Actions = buildMap {
            put("snapshot") { args -> element.driverSnapshot(parseSnapshotOptions(args.toTypedArray())) }
            put("screenshot") { element.driverScreenshot() }
            put("find") { args -> element.driverFind(args.firstOrNull() ?: "", args.contains("--hidden")) }
            put("findClickable") { args -> element.driverFindClickable(args.firstOrNull() ?: "", args.contains("--hidden")) }
            put("scrollIntoView") {
                element.scrollIntoView(Align.Center, Align.Center, animate = false)
                "OK"
            }
            if (element.dragData != null) put("getDragData") {
                val data = element.dragData ?: throw DriverActionException("no dragData on this view")
                val serialized = buildString {
                    append(data.label)
                    for ((mime, value) in data.typeToData) {
                        append('\u0000'); append(mime); append('\u0000'); append(value)
                    }
                }
                Base64.encode(serialized.encodeToByteArray())
            }
            if (element.dropTargetDelegate != null) put("drop") { args ->
                val encoded = args.firstOrNull() ?: throw DriverActionException("drop requires base64 drag data argument")
                val decoded = Base64.decode(encoded).decodeToString()
                val parts = decoded.split('\u0000')
                if(parts.size % 2 == 0) throw DriverActionException("invalid drag data format: expected label followed by mime/value pairs")
                val label = parts[0]
                val typeToData = (1 until parts.size step 2).associate { parts[it] to parts[it + 1] }
                val dragData = DragData(label, typeToData)
                val delegate = element.dropTargetDelegate ?: throw DriverActionException("no drop target found on this view or ancestors")
                val event = DragEvent(dragData, 0.0, 0.0)
                delegate.enter(event)
                val result = delegate.drop(event)
                delegate.end(event)
                if (result) "OK" else throw DriverActionException("drop was rejected by the target")
            }
        }

        public fun defaultDriverDisplay(element: Element, options: Element.DriverSnapshotOptions): String = buildString {
            val name = element.debugName
            if (name != null) {
                append("$name: ")
            } else {
                val idx = element.parent?.children?.indexOf(element)?.takeIf { it >= 0 } ?: 0
                append("$idx: ")
            }
            if (options.includeThemes) {
                element.themeChoice.takeUnless { it == ThemeDerivation.None }?.let { append(it); append(' ') }
            }
            append(element::class.simpleName)
            element.driverValue?.let { append(" = \"$it\"") }
            if (!element.shown) append(" (hidden)")
            else if (!element.visible) append(" (invisible)")
            if (options.includeActions) element.driverActions.keys
                .filter { it != "snapshot" && it != "screenshot" && it != "find" && it != "findClickable" && it != "scrollIntoView" && it != "getAlignment" }
                .takeUnless { it.isEmpty() }
                ?.let { append(" [${it.joinToString(", ")}]") }
        }
    }
}

/**
 * Lazily installs a [MockExternalServices] on the root view's [RContext], wrapping the existing
 * external services as a delegate. Returns the mock instance. Subsequent calls return the same instance.
 */
public fun ensureMockExternalServices(root: Element): MockExternalServices {
    val existing = root.context.externalServices
    if (existing is MockExternalServices) return existing
    val mock = MockExternalServices(delegate = existing)
    root.context.addons[ElementContext::externalServices.name] = mock
    return mock
}

@OptIn(ExperimentalEncodingApi::class)
public suspend fun handleCommand(command: String, root: Element?, navigator: PageNavigator?): String {
    if (command.isBlank()) throw DriverActionException("empty command")

    val parts = command.split('\t')
    val target = parts[0]
    val action = parts.getOrNull(1)
    val args = parts.drop(2).toTypedArray()

    return when (target) {
        "navigate" -> {
            if (navigator == null) throw DriverActionException("no navigator available")
            val route = action ?: throw DriverActionException("no route specified")
            if (!navigator.navigateUrlLikePath(route)) throw DriverActionException("route '$route' not found")
            "OK"
        }
        "reset" -> {
            if (navigator == null) throw DriverActionException("no navigator available")
            val route = action ?: throw DriverActionException("no route specified")
            if (!navigator.resetUrlLikePath(route)) throw DriverActionException("route '$route' not found")
            "OK"
        }
        "url" -> {
            if (navigator == null) throw DriverActionException("no navigator available")
            val currentPage = navigator.stack.value.lastOrNull()
                ?: throw DriverActionException("no current page")
            val rendered = navigator.routes.render(currentPage)
                ?: throw DriverActionException("current page has no route")
            rendered.urlLikePath.render()
        }
        "back" -> {
            if (navigator == null) throw DriverActionException("no navigator available")
            if (navigator.goBack()) "OK" else throw DriverActionException("can't go back, already at root")
        }
        "logs" -> {
            val count = action?.toIntOrNull() ?: 50
            val entries = LogBuffer.recent(count)
            if (entries.isEmpty()) "No log entries"
            else entries.joinToString("\n") { "[${it.level}] ${it.tag}: ${it.msg}" }
        }
        "mock" -> {
            if (root == null) throw DriverActionException("no root view available")
            val mockAction = action ?: throw DriverActionException("no mock action specified")
            val mock = ensureMockExternalServices(root)
            when (mockAction) {
                "file" -> {
                    val base64 = args.getOrNull(0) ?: throw DriverActionException("mockFile requires base64 data")
                    val mimeType = args.getOrNull(1) ?: "application/octet-stream"
                    val fileName = args.getOrNull(2) ?: "mock-file"
                    val bytes = Base64.decode(base64)
                    val ref = createFileReferenceFromBytes(bytes, mimeType, fileName)
                    mock.pendingFileResponses.addLast(ref)
                    "OK"
                }
                "fileNull" -> {
                    mock.pendingFileResponses.addLast(null)
                    "OK"
                }
                "geolocation" -> {
                    val lat = args.getOrNull(0)?.toDoubleOrNull() ?: throw DriverActionException("mockGeolocation requires latitude")
                    val lon = args.getOrNull(1)?.toDoubleOrNull() ?: throw DriverActionException("mockGeolocation requires longitude")
                    val accuracy = args.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                    mock.pendingGeolocation.addLast(GeolocationResult(lat, lon, accuracy))
                    "OK"
                }
                "clearCalls" -> {
                    mock.calls.clear()
                    "OK"
                }
                "calls" -> {
                    if (mock.calls.isEmpty()) "No calls recorded"
                    else mock.calls.joinToString("\n") { it.toString() }
                }
                else -> throw DriverActionException("Unknown mock action '$mockAction'. Available: file, fileNull, geolocation, clearCalls, calls")
            }
        }
        else -> {
            // View-targeted command
            if (root == null) throw DriverActionException("no root view available")
            val view = if (target == "root") root else root.resolveDriverPath(target)
                ?: throw DriverActionException("view '$target' not found")
            val handler = view.driverActions[action ?: throw DriverActionException("no action specified")]
                ?: throw DriverActionException("Unknown action '$action' on ${view::class.simpleName}. Available: ${view.driverActions.keys.joinToString()}")
            handler(args.toList())
        }
    }
}

/** Log interceptor that buffers log calls for the driver's "logs" command. */
private class BufferingLogInterceptor : LogInterceptor {
    override fun intercept(level: LogLevel, tag: String, entries: Array<out Any?>) {
        LogBuffer.add(level.name, tag, entries.joinToString(" "))
    }
}
