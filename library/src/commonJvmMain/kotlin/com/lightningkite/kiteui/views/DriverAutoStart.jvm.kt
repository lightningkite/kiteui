package com.lightningkite.kiteui.views

import java.io.File
import java.net.Socket

internal actual fun tryAutoStartDaemon(port: Int) {
    // Check if already running
    try {
        Socket("127.0.0.1", port).close()
        return // Already running
    } catch (_: Exception) {
        // Not running, try to start
    }

    val jar = File(System.getProperty("user.home"), ".kiteui/ai-driver/kiteui-ai-driver.jar")
    if (!jar.exists()) return // JAR not installed, skip silently

    val logFile = File(System.getProperty("user.home"), ".kiteui/ai-driver/server.log")
    try {
        ProcessBuilder("java", "-jar", jar.absolutePath, port.toString())
            .redirectOutput(logFile)
            .redirectErrorStream(true)
            .start()

        // Poll for up to 5 seconds
        repeat(50) {
            try {
                Socket("127.0.0.1", port).close()
                return // Started successfully
            } catch (_: Exception) {
                Thread.sleep(100)
            }
        }
    } catch (_: Exception) {
        // Failed to start, proceed without daemon
    }
}
