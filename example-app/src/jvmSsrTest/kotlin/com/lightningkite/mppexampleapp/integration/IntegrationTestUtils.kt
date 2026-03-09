// by Claude - shared utilities for platform integration tests
package com.lightningkite.mppexampleapp.integration

import com.lightningkite.kiteui.aidriver.ConnectedApp
import com.lightningkite.kiteui.testing.listApps
import java.net.HttpURLConnection
import java.net.URL

/** The expected app name for the KiteUI example app (must match App.kt's AiDriver.connect call). */
// by Claude
internal const val EXAMPLE_APP_NAME = "KiteUI Sample App"

/** Check if a shell command is available on PATH. */
// by Claude
internal fun isCommandAvailable(command: String): Boolean = try {
    ProcessBuilder("which", command)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
        .waitFor() == 0
} catch (_: Exception) {
    false
}

/** Execute a command and return combined stdout+stderr as a string. */
// by Claude
internal fun exec(vararg command: String): String {
    val process = ProcessBuilder(*command)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return output.trim()
}

/**
 * List connected apps from the daemon. Returns null if the daemon can't be reached.
 * Delegates to [listApps] which handles both JSON and text format responses.
 */
// by Claude
internal fun tryListApps(host: String = "localhost", port: Int = 7475): List<ConnectedApp>? {
    return try {
        listApps(host, port)
    } catch (_: Exception) {
        null
    }
}

/**
 * Check if an HTTP server is responding on the given port.
 * Tries IPv4, IPv6, and "localhost" to handle servers that bind to any one of them.
 */
// by Claude
internal fun isPortOpen(port: Int, host: String = "localhost"): Boolean {
    for (addr in listOf("127.0.0.1", "[::1]", host)) {
        try {
            val conn = URL("http://$addr:$port").openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "HEAD"
            if (conn.responseCode in 100..599) return true
        } catch (_: Exception) {
            // try next address
        }
    }
    return false
}

/**
 * Skip the current test if [condition] is false.
 * Uses JUnit 4's [org.junit.Assume.assumeTrue] to mark the test as "ignored" in reports.
 */
// by Claude
internal fun skipUnless(condition: Boolean, message: String) {
    org.junit.Assume.assumeTrue(message, condition)
}
