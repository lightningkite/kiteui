// by Claude - entry points and convenience tools for running UiTestScope tests against a live app
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.aidriver.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.awt.Desktop
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// -- Daemon utilities --

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Post a raw [CliCommand] to the daemon and return the response text.
 * Throws if the daemon is unreachable.
 */
// by Claude
private fun postDaemonCommand(command: CliCommand, host: String = "localhost", port: Int = 7475): String {
    val body = json.encodeToString(CliCommand.serializer(), command)
    val conn = URL("http://$host:$port/cli").openConnection() as HttpURLConnection
    try {
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.connectTimeout = 5000
        conn.readTimeout = 15000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toByteArray()) }
        return if (conn.responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val err = conn.errorStream?.bufferedReader()?.readText() ?: ""
            throw RuntimeException("Daemon returned HTTP ${conn.responseCode}: $err")
        }
    } finally {
        conn.disconnect()
    }
}

/**
 * Check if the daemon is reachable at the given host/port.
 */
// by Claude
fun isDaemonRunning(host: String = "localhost", port: Int = 7475): Boolean = try {
    val conn = URL("http://$host:$port/cli").openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 2000
    conn.readTimeout = 2000
    conn.doOutput = true
    conn.outputStream.use { it.write("""{"type":"status"}""".toByteArray()) }
    conn.responseCode == 200
} catch (_: Exception) {
    false
}

/**
 * Ensure the daemon is running, auto-starting it if `kiteui-drive` is installed.
 * Throws if the daemon cannot be started.
 */
// by Claude
fun ensureDaemon(host: String = "localhost", port: Int = 7475) {
    if (isDaemonRunning(host, port)) return

    // Only auto-start on localhost
    if (host != "localhost" && host != "127.0.0.1") {
        throw IllegalStateException("Daemon not running at $host:$port. Start it manually with: kiteui-drive start")
    }

    val wsPort = port - 1 // convention: CLI port = WS port + 1
    val wrapper = File(System.getProperty("user.home"), ".kiteui/bin/kiteui-drive")
    if (!wrapper.exists()) {
        throw IllegalStateException(
            "kiteui-drive not installed. Run: ./gradlew aiDriverInstall"
        )
    }
    ProcessBuilder(wrapper.absolutePath, "start", "--port", wsPort.toString())
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
    repeat(20) {
        Thread.sleep(500)
        if (isDaemonRunning(host, port)) return
    }
    throw IllegalStateException("Daemon failed to start within 10 seconds")
}

// -- App discovery --

/**
 * List all currently connected apps from the daemon.
 * Tries JSON format first; falls back to parsing the text format for older daemon builds.
 */
// by Claude
fun listApps(host: String = "localhost", port: Int = 7475): List<ConnectedApp> {
    // Try JSON format first
    try {
        val response = postDaemonCommand(
            CliCommand.List(format = CliCommand.List.ListFormat.Json), host, port
        )
        return json.decodeFromString(ListSerializer(ConnectedApp.serializer()), response)
    } catch (_: Exception) {
        // JSON parse failed — daemon may be an older build without JSON list support
    }
    // Fallback: text format "appId (platform) - appName" per line
    val response = postDaemonCommand(CliCommand.List(), host, port)
    if (response == "No apps connected") return emptyList()
    return response.lines().mapNotNull { line ->
        val match = Regex("""^(.+?) \((.+?)\) - (.+)$""").matchEntire(line.trim())
        match?.let {
            ConnectedApp(
                appId = it.groupValues[1],
                platform = it.groupValues[2],
                appName = it.groupValues[3]
            )
        }
    }
}

/**
 * Wait until an app whose ID starts with [appIdPrefix] connects to the daemon.
 * Returns the full app ID.
 *
 * Useful when the exact app ID isn't known in advance (e.g. browser generates "web" or "web-2").
 *
 * @param appIdPrefix prefix to match (e.g. "web", "android")
 * @param appName optional app name to match (filters by [ConnectedApp.appName])
 * @param timeout how long to wait before giving up
 */
// by Claude
fun waitForApp(
    appIdPrefix: String,
    appName: String? = null,
    host: String = "localhost",
    port: Int = 7475,
    timeout: Duration = 30.seconds
): String = runBlocking {
    withTimeout(timeout) {
        while (true) {
            try {
                val apps = listApps(host, port)
                val match = apps.firstOrNull {
                    it.appId.startsWith(appIdPrefix) && (appName == null || it.appName == appName)
                }
                if (match != null) return@withTimeout match.appId
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e // don't swallow timeout cancellation
            } catch (_: Exception) {
                // daemon not ready yet, retry
            }
            delay(500)
        }
        @Suppress("UNREACHABLE_CODE")
        error("unreachable")
    }
}

// -- Browser launch --

/**
 * Open a URL in the system's default browser.
 * Uses [Desktop.browse] on supported platforms, falls back to `xdg-open` / `open` on Mac.
 */
// by Claude
fun openBrowser(url: String) {
    val uri = URI(url)
    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        Desktop.getDesktop().browse(uri)
    } else {
        // Fallback for headless / Linux environments
        val os = System.getProperty("os.name").lowercase()
        val cmd = when {
            os.contains("mac") -> "open"
            os.contains("win") -> "cmd /c start"
            else -> "xdg-open"
        }
        Runtime.getRuntime().exec(arrayOf(cmd, url))
    }
}

// -- Combined entry points --

/**
 * Run a [UiTestScope] test against a live, already-running app connected to the ai-driver daemon.
 *
 * Prerequisites:
 * - Daemon running (`kiteui-drive start`)
 * - App connected (`AiDriver.connect()` called in the app)
 *
 * Example:
 * ```kotlin
 * class LiveAppTest {
 *     @Test
 *     fun dashboardLoads() = remoteUiTest(appId = "web") {
 *         navigate("/dashboard")
 *         waitForPage("DashboardPage")
 *         assertVisible("dashboardTitle")
 *     }
 * }
 * ```
 *
 * @param appId The connected app ID (e.g. "web", "android-1"). Use `kiteui-drive list` to see connected apps.
 * @param host Daemon host (default: localhost)
 * @param port Daemon CLI port (default: 7475)
 * @param block The test body using [UiTestScope] API
 */
fun remoteUiTest(
    appId: String,
    host: String = "localhost",
    port: Int = 7475,
    block: suspend UiTestScope.() -> Unit
) {
    val backend = RemoteUiTestBackend(appId, host, port)
    val scope = UiTestScope(backend)
    runBlocking { scope.block() }
}

/**
 * Open a web app in the browser, wait for it to connect to the daemon, then run a test against it.
 *
 * This is the highest-level convenience for web app testing. It:
 * 1. Ensures the daemon is running (auto-starts if needed)
 * 2. Opens [url] in the default browser
 * 3. Waits for an app with ID starting with [appIdPrefix] to connect
 * 4. Runs the test block against the connected app
 *
 * Example:
 * ```kotlin
 * class WebAppTest {
 *     @Test
 *     fun homePageLoads() = remoteWebUiTest(
 *         url = "http://localhost:8080",
 *         appIdPrefix = "web"
 *     ) {
 *         waitForPage("HomePage")
 *         assertVisible("title")
 *         screenshotToFile("screenshots/home.png")
 *     }
 * }
 * ```
 *
 * @param url The URL to open in the browser
 * @param appIdPrefix prefix to match the connected app ID (default: "web")
 * @param appName optional app name filter for [waitForApp]
 * @param host Daemon host
 * @param port Daemon CLI port
 * @param connectTimeout How long to wait for the app to connect
 * @param block The test body
 */
// by Claude
fun remoteWebUiTest(
    url: String,
    appIdPrefix: String = "web",
    appName: String? = null,
    host: String = "localhost",
    port: Int = 7475,
    connectTimeout: Duration = 30.seconds,
    block: suspend UiTestScope.() -> Unit
) {
    ensureDaemon(host, port)
    openBrowser(url)
    val appId = waitForApp(appIdPrefix, appName = appName, host = host, port = port, timeout = connectTimeout)
    remoteUiTest(appId = appId, host = host, port = port, block = block)
}

// -- Screenshot convenience --

/**
 * Capture a screenshot and save it to a file on disk.
 * Only works with backends that support screenshots (e.g. [RemoteUiTestBackend]).
 *
 * Useful for capturing app store screenshots from live Android/iOS apps:
 * ```kotlin
 * remoteUiTest(appId = "android-1") {
 *     navigate("/home")
 *     waitForPage("HomePage")
 *     screenshotToFile("screenshots/home-android.png")
 * }
 * ```
 *
 * @param path file path to save the PNG to
 * @throws UnsupportedOperationException if the backend doesn't support screenshots
 */
// by Claude - convenience for saving screenshots to disk from remote tests
suspend fun UiTestScope.screenshotToFile(path: String) {
    val bytes = screenshot()
        ?: throw UnsupportedOperationException("Screenshots not supported by this backend")
    val file = File(path)
    file.parentFile?.mkdirs()
    file.writeBytes(bytes)
}
