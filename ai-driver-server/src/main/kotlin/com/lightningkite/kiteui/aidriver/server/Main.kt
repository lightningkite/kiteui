// by Claude - entry point: delegates to daemon or sends CLI command to daemon
package com.lightningkite.kiteui.aidriver.server

import com.lightningkite.kiteui.aidriver.CliCommand
import com.lightningkite.kiteui.aidriver.CliFormat
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        return
    }

    val command = try {
        CliFormat.parse(args.toList())
    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
        printUsage()
        return
    }

    when (command) {
        is CliCommand.Start -> {
            Daemon.start(command.port, command.cliPort, command.daemon)
        }
        else -> {
            // Forward to running daemon via HTTP
            sendCliCommand(command, 7475)
        }
    }
}

private fun sendCliCommand(command: CliCommand, cliPort: Int) {
    try {
        val json = Json { encodeDefaults = true }
        val body = json.encodeToString(CliCommand.serializer(), command)
        val url = URL("http://localhost:$cliPort/cli")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        conn.outputStream.use { it.write(body.toByteArray()) }
        val response = conn.inputStream.bufferedReader().readText()
        println(response)
    } catch (e: java.net.ConnectException) {
        System.err.println("Error: AI driver daemon is not running. Start it with: java -jar ai-driver-server.jar start")
    }
}

private fun printUsage() {
    println("""
        AI Driver CLI - LLM-driven UI automation for KiteUI apps

        Usage: ui <command> [options]

        Commands:
          start [--port 7474] [--cliPort 7475] [--daemon]              Start the daemon
          stop                                                          Stop the daemon
          status                                                        Show daemon status
          list                                                          List connected apps
          info <appId>                                                  Show app info
          snapshot <appId> [--component path] [--format Text|Json]     Get UI snapshot
          screenshot <appId> [--format SaveToFile|Base64]               Take screenshot (saves to ~/.kiteui/screenshots/)
          perform <appId> --action <click:id|longClick:id|setValue:id:val|scroll:id|navigate:route|back|forward>
                                                                        Perform a UI action
          wait <appId> [--page PageName] [--component id]              Wait for condition
          record <appId> <start|stop|export|export-kotlin>             Record interactions
    """.trimIndent())
}
