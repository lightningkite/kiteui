// by Claude - CLI command types for LLM-driven UI automation
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * All commands that can be issued to the AI driver CLI.
 * These are serializable so they can be used both from the command line
 * and directly in unit tests via [CliFormat].
 */
@Serializable
sealed class CliCommand {
    /** List all currently connected app instances. */
    // by Claude - added format for structured JSON responses
    @Serializable @SerialName("list")
    data class List(val format: ListFormat = ListFormat.Text) : CliCommand() {
        @Serializable
        enum class ListFormat { Text, Json }
    }

    /** Show info about a specific connected app. */
    @Serializable @SerialName("info")
    data class Info(val appId: String) : CliCommand()

    /**
     * Capture and print the UI component tree for a connected app.
     * @param component optional sub-path to scope the snapshot
     * @param format if true, output plain text instead of JSON
     */
    @Serializable @SerialName("snapshot")
    data class Snapshot(
        val appId: String,
        val component: String? = null,
        val search: String? = null, // by Claude - filter to components whose value contains this text
        val settings: UiSnapshotSettings = UiSnapshotSettings(),
        val format: Format = Format.Text
    ) : CliCommand() {
        @Serializable
        enum class Format { Text, Json }
    }

    /**
     * Capture a screenshot from the app.
     * @param path local file path to save PNG (CLI client writes the file, not the server)
     * @param format SaveToFile saves to [path] on the CLI client; Base64 returns raw base64
     */
    // by Claude - path is handled by CLI client, not the server (prevents path traversal via daemon)
    @Serializable @SerialName("screenshot")
    data class Screenshot(
        val appId: String,
        val path: String? = null,
        val format: Format = Format.SaveToFile
    ) : CliCommand() {
        @Serializable
        enum class Format { SaveToFile, Base64 }
    }

    /** Click a UI component by its absolute path ID. */
    @Serializable @SerialName("perform")
    data class Perform(val appId: String, val action: UiAction) : CliCommand()

    /**
     * Wait until a condition is met (or timeout expires).
     * All conditions must be met simultaneously if multiple are specified.
     */
    @Serializable @SerialName("wait")
    data class Wait(
        val appId: String,
        val page: String? = null,
        val url: String? = null,
        val component: String? = null,
        val componentGone: String? = null,
        val enabled: String? = null,
        val change: Boolean = false,
        val timeout: Long = 10_000
    ) : CliCommand()

    /**
     * Control recording of interactions for replay / test export.
     * @param action one of: start, stop, export, export-kotlin
     */
    @Serializable @SerialName("record")
    data class Record(val appId: String, val action: String) : CliCommand()

    // by Claude - queue a mock response on a connected app's external services
    @Serializable @SerialName("mock")
    data class Mock(val appId: String, val mockType: MockType) : CliCommand()

    // by Claude - fetch buffered log entries from a connected app
    @Serializable @SerialName("logs")
    data class Logs(
        val appId: String,
        val lines: Int = 200,
        val level: LogLevel? = null,
        val tag: String? = null,
        val format: Format = Format.Text // by Claude - JSON format for remote test backend
    ) : CliCommand() {
        @Serializable
        enum class Format { Text, Json }
    }

    /** Start the daemon process. */
    @Serializable @SerialName("start")
    data class Start(
        val port: Int = 7474,
        val cliPort: Int = 7475,
        val daemon: Boolean = false
    ) : CliCommand()

    /** Stop a running daemon. */
    @Serializable @SerialName("stop")
    data object Stop : CliCommand()

    /** Check daemon status. */
    @Serializable @SerialName("status")
    data object Status : CliCommand()
}

// by Claude - structured app info returned by List command in JSON format
@Serializable
data class ConnectedApp(
    val appId: String,
    val platform: String,
    val appName: String
)

// by Claude - types of mock responses that can be queued via CLI
@Serializable
sealed class MockType {
    /** Queue a mock file response (works for file picker, camera capture, etc.). CLI: `./ui mock <app> file <path> [--mimeType ...]` */
    @Serializable @SerialName("file")
    data class File(val path: String, val mimeType: String? = null) : MockType()

    /** Queue a mock geolocation response. CLI: `./ui mock <app> geolocation <lat> <lng> [--accuracy ...]` */
    @Serializable @SerialName("geolocation")
    data class Geolocation(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Double = 10.0
    ) : MockType()
}
