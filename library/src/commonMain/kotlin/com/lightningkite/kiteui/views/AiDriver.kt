package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kotlinx.serialization.uri.encodeURIComponent
import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.launch

/**
 * AI Driver client — connects to a relay server via WebSocket and handles text commands.
 * Each command is a plain string; the response is a plain string sent back on the same socket.
 */
object AiDriver {
    private val log = LogRoot.tag("AiDriver")

    fun connect(
        appName: String,
        platform: String = Platform.current.name.lowercase(),
        postfix: String? = buildString {
            append(('A'..'Z').random())
            append(('A'..'Z').random())
            append(('A'..'Z').random())
        },
        host: String = "localhost",
        port: Int = 7474,
        rootView: () -> RView?,
        navigator: () -> PageNavigator?,
    ) {
        tryAutoStartDaemon(port)

        // Install log interceptor that forwards to LogRoot AND buffers
        if (logInterceptor == null) {
            logInterceptor = BufferingLogInterceptor(logInterceptor ?: LogRoot)
        }

        val gate = ConnectivityGate()
        val ws = retryWebsocket(
            url = "ws://$host:$port?app=${encodeURIComponent(appName)}&platform=${encodeURIComponent(platform)}&postfix=${encodeURIComponent(postfix ?: "x")}",
            pingTime = 30_000,
            gate = gate,
        )

        ws.onOpen {
            log.info("Connected to driver server at $host:$port as $appName-$platform")
        }
        ws.onClose {
            log.info("Driver server disconnected")
        }

        ws.onMessage { message ->
            AppScope.launch {
                try {
                    val response = handleCommand(message.trim(), rootView(), navigator())
                    ws.send(response)
                } catch (e: Exception) {
                    ws.send("ERROR: ${e::class.simpleName}: ${e.message}")
                }
            }
        }

        // Activate the WebSocket — retryWebsocket uses lazy connection via beginUse()
        ws.beginUse()
    }
}

suspend fun handleCommand(command: String, root: RView?, navigator: PageNavigator?): String {
    if (command.isBlank()) throw DriverActionException("empty command")

    val parts = command.split('\t')
    val target = parts[0]
    val action = parts.getOrNull(1)
    val args = parts.drop(2).toTypedArray()

    return when (target) {
        "navigate" -> {
            if (navigator == null) throw DriverActionException("no navigator available")
            val route = action ?: throw DriverActionException("no route specified")
            navigator.navigateUrlLikePath(route) ?: throw DriverActionException("route '$route' not found")
            "OK"
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

/**
 * Log interceptor that forwards all calls to [LogRoot] and also buffers them in [LogBuffer].
 */
private class BufferingLogInterceptor(val wraps: Log, val tag: String = "") : Log {
    override fun tag(tag: String): Log = BufferingLogInterceptor(wraps.tag(tag), tag)
    override fun log(vararg entries: Any?) {
        wraps.log(*entries)
        LogBuffer.add("LOG", tag, entries.joinToString(" "))
    }
    override fun error(vararg entries: Any?) {
        wraps.error(*entries)
        LogBuffer.add("ERROR", tag, entries.joinToString(" "))
    }
    override fun info(vararg entries: Any?) {
        wraps.info(*entries)
        LogBuffer.add("INFO", tag, entries.joinToString(" "))
    }
    override fun warn(vararg entries: Any?) {
        wraps.warn(*entries)
        LogBuffer.add("WARN", tag, entries.joinToString(" "))
    }
}
