// by Claude - HTTP handler for CLI commands from the command line tool
package com.lightningkite.kiteui.aidriver.server

import com.lightningkite.kiteui.aidriver.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true; prettyPrint = true }

// Per-app recordings, keyed by appId — ConcurrentHashMap for thread safety (by Claude)
private val recordings = java.util.concurrent.ConcurrentHashMap<String, Recording>()

fun Route.cliRoutes() {
    post("/cli") {
        val body = call.receiveText()
        val result = try {
            val command = json.decodeFromString(CliCommand.serializer(), body)
            handleCommand(command)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // by Claude - don't swallow CancellationException, breaks structured concurrency
        } catch (e: Exception) {
            // by Claude - surface errors as readable text instead of opaque 500
            "ERROR: ${e::class.simpleName}: ${e.message}"
        }
        call.respondText(result)
    }
}

private suspend fun handleCommand(command: CliCommand): String {
    return when (command) {
        is CliCommand.ListApps -> {
            val apps = AppRegistry.all()
            // by Claude - JSON format for programmatic access from remote test backend
            if (command.format == CliCommand.ListApps.ListFormat.Json) {
                json.encodeToString(
                    ListSerializer(ConnectedApp.serializer()),
                    apps.map { ConnectedApp(it.appId, it.platform, it.appName) }
                )
            } else if (apps.isEmpty()) "No apps connected"
            else apps.joinToString("\n") { "${it.appId} (${it.platform}) - ${it.appName}" }
        }
        is CliCommand.Info -> {
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            "App: ${app.appName}\nPlatform: ${app.platform}\nID: ${app.appId}"
        }
        // by Claude - server-side snapshot filtering for --component, --search, --interactiveOnly
        is CliCommand.Snapshot -> {
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            var snapshot = app.requestSnapshot()

            // Scope to subtree if --component specified
            command.component?.let { componentId ->
                val found = snapshot.findById(componentId)
                    ?: return "Component '$componentId' not found.\nCurrent snapshot:\n${formatSnapshotText(snapshot)}"
                snapshot = snapshot.copy(components = listOf(found))
            }

            // Filter by value text if --search specified
            command.search?.let { searchText ->
                val filtered = filterBySearch(snapshot, searchText)
                if (filtered.components.isEmpty()) {
                    return "No components matching '$searchText' found.\nFull snapshot:\n${formatSnapshotText(snapshot)}"
                }
                snapshot = filtered
            }

            // by Claude - prune structural-only containers when --interactiveOnly
            if (command.interactiveOnly) {
                snapshot = snapshot.compact()
            }

            if (command.format == CliCommand.Snapshot.Format.Text) formatSnapshotText(snapshot)
            else json.encodeToString(UiSnapshot.serializer(), snapshot)
        }
        // by Claude - server always returns base64; file writing is the CLI client's responsibility
        is CliCommand.Screenshot -> {
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            val result = app.requestScreenshot()
            if (result.error != null) return "Screenshot failed: ${result.error}"
            result.base64 ?: return "No screenshot data returned"
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
        // by Claude - fetch and display buffered log entries from a connected app
        is CliCommand.Logs -> {
            val app = AppRegistry.get(command.appId) ?: return "App '${command.appId}' not found"
            var entries = app.requestLogs(command.lines)
            command.level?.let { minLevel ->
                entries = entries.filter { it.level >= minLevel }
            }
            command.tag?.let { tagFilter ->
                entries = entries.filter { tagFilter in it.tag }
            }
            // by Claude - support JSON format for remote test backend
            if (command.format == CliCommand.Logs.Format.Json) {
                json.encodeToString(ListSerializer(LogEntry.serializer()), entries)
            } else if (entries.isEmpty()) "No log entries"
            else entries.joinToString("\n") { e ->
                val ts = java.time.Instant.ofEpochMilli(e.timestamp).toString().substringAfter("T").substringBefore("Z")
                "${ts} [${e.level.name.uppercase().padEnd(5)}] ${if (e.tag.isNotEmpty()) "${e.tag}: " else ""}${e.message}"
            }
        }
        // by Claude - structured component search: find components matching criteria
        is CliCommand.Find -> {
            // by Claude - require at least one filter to avoid returning arbitrary components
            if (command.value == null && command.type == null && command.action == null && command.id == null) {
                return "ERROR: At least one filter (--value, --type, --action, --id) is required"
            }
            val app = AppRegistry.get(command.appId)
                ?: return "App '${command.appId}' not found"
            val snapshot = app.requestSnapshot()
            // by Claude - delegates to UiSnapshot.find() for shared logic with tests
            val results = snapshot.find(
                value = command.value,
                type = command.type,
                action = command.action,
                id = command.id,
                limit = command.limit
            )
            // by Claude - on no match, return snapshot so caller can see what's there
            if (results.isEmpty()) {
                return "NOT_FOUND: No components matching criteria.\n${formatSnapshotText(snapshot)}"
            }
            if (command.format == CliCommand.Find.Format.Json) {
                json.encodeToString(ListSerializer(FindResult.serializer()), results)
            } else {
                results.joinToString("\n") { r ->
                    val parts = mutableListOf(r.id)
                    parts.add(r.type)
                    r.value?.let { parts.add("= \"$it\"") }
                    if (r.actions.isNotEmpty()) parts.add("[${r.actions.joinToString(",")}]")
                    if (!r.enabled) parts.add("(disabled)")
                    parts.joinToString(" ")
                }
            }
        }
        is CliCommand.Status -> "Daemon running. Connected apps: ${AppRegistry.all().size}"
        is CliCommand.Stop -> {
            // Signal shutdown via a background thread so the response can be sent first
            Thread {
                Thread.sleep(200)
                kotlin.system.exitProcess(0)
            }.also { it.isDaemon = true }.start()
            "Stopping daemon..."
        }
        is CliCommand.Start -> "Daemon already running"
    }
}

// by Claude - keep only components whose subtree contains a value match, preserving tree structure
private fun filterBySearch(snapshot: UiSnapshot, text: String): UiSnapshot {
    fun filterComponent(comp: UiComponent): UiComponent? {
        val matchingSelf = comp.value?.contains(text, ignoreCase = true) == true
        val filteredChildren = comp.children.mapNotNull { filterComponent(it) }
        return if (matchingSelf || filteredChildren.isNotEmpty()) {
            comp.copy(children = if (matchingSelf) comp.children else filteredChildren)
        } else null
    }
    val filtered = snapshot.components.mapNotNull { filterComponent(it) }
    return snapshot.copy(components = filtered)
}

// by Claude - compact text format unified with UiComponent.render()
private fun formatSnapshotText(snapshot: UiSnapshot): String = buildString {
    appendLine("Page: ${snapshot.page} (${snapshot.url})")
    snapshot.components.forEach { it.render(this, 0) }
}
