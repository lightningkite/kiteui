// by Claude - remote backend for UiTestScope, sends commands to a live app via the ai-driver daemon
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.aidriver.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * [UiTestBackend] that sends commands to a live app via the ai-driver daemon HTTP API.
 *
 * The daemon must be running and the app must be connected via `AiDriver.connect()`.
 *
 * @param appId The connected app ID (e.g. "web", "android-1")
 * @param host Daemon host (default: localhost)
 * @param port Daemon CLI port (default: 7475)
 */
class RemoteUiTestBackend(
    val appId: String,
    val host: String = "localhost",
    val port: Int = 7475,
) : UiTestBackend {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val baseUrl get() = "http://$host:$port/cli"

    private fun postCommand(command: CliCommand): String {
        val body = json.encodeToString(CliCommand.serializer(), command)
        val url = URL(baseUrl)
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.outputStream.use { it.write(body.toByteArray()) }
            val responseCode = conn.responseCode
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                throw RuntimeException("Daemon returned HTTP $responseCode: $errorBody")
            }
            return responseBody
        } finally {
            conn.disconnect()
        }
    }

    override suspend fun snapshot(): UiSnapshot {
        val response = postCommand(
            CliCommand.Snapshot(appId = appId, format = CliCommand.Snapshot.Format.Json)
        )
        return json.decodeFromString(UiSnapshot.serializer(), response)
    }

    override suspend fun perform(action: UiAction): ActionDispatchResult {
        val response = postCommand(CliCommand.Perform(appId = appId, action = action))
        return if (response == "OK") {
            ActionDispatchResult(success = true)
        } else if (response.startsWith("Failed: ")) {
            ActionDispatchResult(success = false, error = response.removePrefix("Failed: "))
        } else {
            // Unexpected response — treat as error
            ActionDispatchResult(success = false, error = response)
        }
    }

    override suspend fun logs(lines: Int): List<LogEntry> {
        val response = postCommand(
            CliCommand.Logs(appId = appId, lines = lines, format = CliCommand.Logs.Format.Json)
        )
        return json.decodeFromString(ListSerializer(LogEntry.serializer()), response)
    }

    // by Claude - screenshot support for remote testing (app store screenshots, etc.)
    override suspend fun screenshot(): ByteArray {
        val base64 = postCommand(
            CliCommand.Screenshot(appId = appId, format = CliCommand.Screenshot.Format.Base64)
        )
        if (base64.startsWith("Screenshot failed:") || base64.startsWith("App '") || base64.startsWith("No screenshot")) {
            throw RuntimeException(base64)
        }
        return java.util.Base64.getDecoder().decode(base64)
    }

    // by Claude - mock support for remote tests, sends bytes to daemon which forwards to app
    override suspend fun mockFile(bytes: ByteArray, mimeType: String, fileName: String) {
        // Write bytes to a temp file so the daemon can read it via CliCommand.Mock
        val tempFile = java.io.File.createTempFile("kiteui-mock-", "-$fileName")
        try {
            tempFile.writeBytes(bytes)
            val response = postCommand(
                CliCommand.Mock(appId = appId, mockType = MockType.File(tempFile.absolutePath, mimeType))
            )
            if (response.startsWith("Failed:") || response.startsWith("ERROR:")) {
                throw RuntimeException("Mock file failed: $response")
            }
        } finally {
            tempFile.delete()
        }
    }

    // by Claude - explicit capture mock routes to pendingCaptureResponses
    override suspend fun mockCapture(bytes: ByteArray, mimeType: String, fileName: String) {
        val tempFile = java.io.File.createTempFile("kiteui-mock-", "-$fileName")
        try {
            tempFile.writeBytes(bytes)
            val response = postCommand(
                CliCommand.Mock(appId = appId, mockType = MockType.Capture(tempFile.absolutePath, mimeType))
            )
            if (response.startsWith("Failed:") || response.startsWith("ERROR:")) {
                throw RuntimeException("Mock capture failed: $response")
            }
        } finally {
            tempFile.delete()
        }
    }

    override suspend fun mockGeolocation(latitude: Double, longitude: Double, accuracyInMeters: Double) {
        val response = postCommand(
            CliCommand.Mock(appId = appId, mockType = MockType.Geolocation(latitude, longitude, accuracyInMeters))
        )
        if (response.startsWith("Failed:") || response.startsWith("ERROR:")) {
            throw RuntimeException("Mock geolocation failed: $response")
        }
    }
}
