package com.lightningkite.kiteui.aidriver

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.WebSockets as ServerWebSockets
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlin.test.*

/**
 * Integration test for the AI driver relay server.
 * Starts the real server, connects a simulated app via WebSocket, and verifies
 * that CLI commands (HTTP POST) are relayed to the app and responses returned.
 */
class ServerIntegrationTest {
    private val port = 17474 // Avoid conflict with any running server

    @Test
    fun fullRelayRoundTrip() = runBlocking {
        // Clear any state from previous runs
        apps.clear()

        // Start the server with a reference so we can stop it
        val server = embeddedServer(Netty, port = port, host = "127.0.0.1") {
            install(ServerWebSockets)
            routing {
                configureRouting()
            }
        }.start(wait = false)
        delay(1000) // Let server start

        val client = HttpClient(CIO) {
            install(io.ktor.client.plugins.websocket.WebSockets)
        }

        try {
            // 1. Verify `ls` with no apps connected
            val lsEmpty = client.post("http://127.0.0.1:$port") {
                setBody("ls")
            }.bodyAsText()
            assertEquals("(no apps connected)", lsEmpty)
            println("PASS: ls with no apps")

            // 2. Connect a simulated app via WebSocket that echoes commands
            val appJob = launch(Dispatchers.IO) {
                client.webSocket("ws://127.0.0.1:$port?app=testapp&platform=jvm") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            send("ECHO: $text")
                        }
                    }
                }
            }
            delay(500) // Let WS connect

            // 3. Verify `ls` now shows the app
            val lsConnected = client.post("http://127.0.0.1:$port") {
                setBody("ls")
            }.bodyAsText()
            println("ls result: $lsConnected")
            assertTrue(lsConnected.contains("testapp-jvm"), "Should list app: $lsConnected")
            println("PASS: ls with connected app")

            // 4. Send a command and verify relay round-trip
            val response = client.post("http://127.0.0.1:$port") {
                setBody("testapp-jvm-x\troot\tsnapshot")
            }.bodyAsText()
            assertEquals("ECHO: root\tsnapshot", response, "Should relay command and return response")
            println("PASS: command relay")

            // 5. Prefix matching works
            val response2 = client.post("http://127.0.0.1:$port") {
                setBody("testapp\troot\tsnapshot\t--interactive")
            }.bodyAsText()
            assertEquals("ECHO: root\tsnapshot\t--interactive", response2, "Prefix match should work")
            println("PASS: prefix matching")

            // 6. Unknown app returns error
            val errorResp = client.post("http://127.0.0.1:$port") {
                setBody("nonexistent\troot\tsnapshot")
            }.bodyAsText()
            assertTrue(errorResp.startsWith("ERROR:"), "Unknown app should error: $errorResp")
            println("PASS: unknown app error")

            // 7. Multiple commands in sequence
            val resp3 = client.post("http://127.0.0.1:$port") {
                setBody("testapp-jvm-x\tmyButton\tclick")
            }.bodyAsText()
            assertEquals("ECHO: myButton\tclick", resp3)

            val resp4 = client.post("http://127.0.0.1:$port") {
                setBody("testapp-jvm-x\temail\tsetValue\ttest@example.com")
            }.bodyAsText()
            assertEquals("ECHO: email\tsetValue\ttest@example.com", resp4)
            println("PASS: sequential commands")

            // Clean up
            appJob.cancelAndJoin()
            println("All integration tests passed!")
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }
}
