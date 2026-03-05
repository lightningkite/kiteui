// by Claude - JVM can launch the daemon process, useful for test harness auto-start
package com.lightningkite.kiteui.aidriver

import java.io.File
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL

internal actual fun tryAutoStartDaemon(port: Int) {
    // Check if already running via CLI HTTP endpoint
    val cliPort = port + 1
    try {
        val conn = URL("http://localhost:$cliPort/cli").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        conn.doOutput = true
        conn.outputStream.use { it.write("""{"type":"status"}""".toByteArray()) }
        if (conn.responseCode == 200) return  // already running
    } catch (_: Exception) {
        // not running, try to start
    }

    val home = System.getProperty("user.home")
    val wrapper = File("$home/.kiteui/bin/kiteui-drive")
    if (!wrapper.exists()) {
        println("[AiDriver] kiteui-drive not found at ${wrapper.absolutePath}. Run: ./gradlew aiDriverInstall")
        return
    }

    ProcessBuilder(wrapper.absolutePath, "start", "--port", port.toString())
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()

    // Brief wait for daemon to start accepting connections
    repeat(10) {
        Thread.sleep(500)
        try {
            Socket("localhost", port).close()
            return  // daemon is up
        } catch (_: Exception) { /* not yet */ }
    }
    println("[AiDriver] Warning: daemon may not have started")
}
