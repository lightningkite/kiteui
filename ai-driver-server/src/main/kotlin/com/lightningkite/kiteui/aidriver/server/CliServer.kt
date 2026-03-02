// by Claude - HTTP handler for CLI commands from the command line tool
package com.lightningkite.kiteui.aidriver.server

import com.lightningkite.kiteui.aidriver.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; prettyPrint = true }

// Per-app recordings, keyed by appId
private val recordings = mutableMapOf<String, Recording>()

fun Route.cliRoutes() {
    post("/cli") {
        val body = call.receiveText()
        val result = try {
            val command = json.decodeFromString(CliCommand.serializer(), body)
            handleCommand(command)
        } catch (e: Exception) {
            // by Claude - surface errors as readable text instead of opaque 500
            "ERROR: ${e::class.simpleName}: ${e.message}"
        }
        call.respondText(result)
    }
}

private suspend fun handleCommand(command: CliCommand): String {
    return when (command) {
        is CliCommand.List -> {
            val apps = AppRegistry.all()
            if (apps.isEmpty()) "No apps connected"
            else apps.joinToString("\n") { "${it.appId} (${it.platform}) - ${it.appName}" }
        }
        is CliCommand.Info -> {
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            "App: ${app.appName}\nPlatform: ${app.platform}\nID: ${app.appId}"
        }
        is CliCommand.Snapshot -> {
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            val snapshot = app.requestSnapshot(command.component)
            if (command.format == CliCommand.Snapshot.Format.Text) formatSnapshotText(snapshot)
            else json.encodeToString(UiSnapshot.serializer(), snapshot)
        }
        is CliCommand.Screenshot -> {
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            val result = app.requestScreenshot()
            if (result.error != null) return "Screenshot failed: ${result.error}"
            val base64 = result.base64 ?: return "No screenshot data returned"
            val bytes = java.util.Base64.getDecoder().decode(base64)
            val path = command.path ?: "screenshot-${command.appId}-${System.currentTimeMillis()}.png"
            java.io.File(path).writeBytes(bytes)
            "Screenshot saved to: $path"
        }
        is CliCommand.Perform -> {
            val app = AppRegistry.get(command.appId) ?: return "App '${command.appId}' not found"
            val result = app.performAction(command.action) as? AppMessage.ActionResult
                ?: return "Unexpected response type from app"
            recordings[command.appId]?.record(command)
            if (result.error != null) "Failed: ${result.error}" else "OK"
        }
        is CliCommand.Wait -> {
            val app = AppRegistry.get(command.appId) ?: return "App '${command.appId}' not found"
            try {
                val snapshot = app.waitFor(
                    page = command.page,
                    url = command.url,
                    component = command.component,
                    componentGone = command.componentGone,
                    enabled = command.enabled,
                    change = command.change,
                    timeout = command.timeout
                )
                "Condition met. Page: ${snapshot.page}"
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                "Timeout waiting for condition"
            }
        }
        is CliCommand.Record -> {
            when (command.action) {
                "start" -> {
                    val r = Recording()
                    r.start()
                    recordings[command.appId] = r
                    "Recording started for ${command.appId}"
                }
                "stop" -> {
                    recordings[command.appId]?.stop()
                    "Recording stopped"
                }
                "export" -> recordings[command.appId]?.export() ?: "No recording found"
                "export-kotlin" -> recordings[command.appId]?.exportKotlin() ?: "No recording found"
                else -> "Unknown record action: ${command.action}"
            }
        }
        // by Claude - handle mock injection for connected apps
        is CliCommand.Mock -> {
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            val requestId = "mock-${System.currentTimeMillis()}"
            when (val mockType = command.mockType) {
                is MockType.File -> {
                    val file = java.io.File(mockType.path)
                    if (!file.exists()) return "File not found: ${mockType.path}"
                    val bytes = file.readBytes()
                    val base64 = java.util.Base64.getEncoder().encodeToString(bytes)
                    val mimeType = mockType.mimeType
                        ?: java.nio.file.Files.probeContentType(file.toPath())
                        ?: "application/octet-stream"
                    val result = app.sendAndAwait(
                        DaemonMessage.QueueMockFile(requestId, base64, mimeType, file.name),
                        requestId
                    )
                    if (result.error != null) "Failed: ${result.error}" else "OK: queued file mock '${file.name}' ($mimeType)"
                }
                is MockType.Geolocation -> {
                    val result = app.sendAndAwait(
                        DaemonMessage.QueueMockGeolocation(requestId, mockType.latitude, mockType.longitude, mockType.accuracy),
                        requestId
                    )
                    if (result.error != null) "Failed: ${result.error}" else "OK: queued geolocation mock (${mockType.latitude}, ${mockType.longitude})"
                }
            }
        }
        is CliCommand.Status -> "Daemon running. Connected apps: ${AppRegistry.all().size}"
        is CliCommand.Stop -> {
            // Signal shutdown via a background thread so the response can be sent first
            Thread {
                Thread.sleep(200)
                System.exit(0)
            }.also { it.isDaemon = true }.start()
            "Stopping daemon..."
        }
        is CliCommand.Start -> "Daemon already running"
    }
}

private fun formatSnapshotText(snapshot: UiSnapshot): String = buildString {
    appendLine("Page: ${snapshot.page} (${snapshot.url})")
    fun formatComponent(comp: UiComponent, indent: String) {
        val info = buildList {
            comp.value?.let { add("value=\"$it\"") }
            if (!comp.enabled) add("disabled")
            if (!comp.visible) add("hidden")
            if (comp.actions.isNotEmpty()) add("actions=[${comp.actions.joinToString(",")}]")
        }.joinToString(", ")
        appendLine("$indent[${comp.id}] ${comp.type}${if (info.isNotEmpty()) " ($info)" else ""}")
        comp.children.forEach { formatComponent(it, "$indent  ") }
    }
    snapshot.components.forEach { formatComponent(it, "") }
}
