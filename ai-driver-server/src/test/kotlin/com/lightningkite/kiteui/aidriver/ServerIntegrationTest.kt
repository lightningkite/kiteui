package com.lightningkite.kiteui.aidriver

import com.lightningkite.lightningserver.websockets.WebSocketFrame
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * Tests for the AI driver relay server business logic.
 * Tests the framework-agnostic functions directly with mock AppConnections.
 * JSON-RPC protocol handling is delegated to Lightning Server's JsonRpcHandler (tested upstream).
 */
class ServerIntegrationTest {

    /** Creates a mock echo app and registers it. The send lambda launches a response asynchronously. */
    private fun CoroutineScope.mockEchoApp(
        id: String = "testapp-jvm-x",
        handler: (String) -> String = { "ECHO: $it" }
    ): AppConnection {
        lateinit var conn: AppConnection
        conn = AppConnection(id, send = { frame ->
            val text = (frame as WebSocketFrame.Text).content
            launch { conn.pending?.complete(handler(text)) }
        })
        apps[id] = conn
        return conn
    }

    @BeforeTest
    fun setup() {
        apps.clear()
    }

    // ===================== CLI Tests =====================

    @Test
    fun lsWithNoApps() = runBlocking {
        assertEquals("(no apps connected)", processCliCommand("ls"))
    }

    @Test
    fun lsWithConnectedApp() = runBlocking {
        mockEchoApp()
        val result = processCliCommand("ls")
        assertTrue(result.contains("testapp-jvm-x"))
    }

    @Test
    fun commandRelay() = runBlocking {
        mockEchoApp()
        val result = processCliCommand("testapp-jvm-x\troot\tsnapshot")
        assertEquals("ECHO: root\tsnapshot", result)
    }

    @Test
    fun prefixMatching() = runBlocking {
        mockEchoApp()
        val result = processCliCommand("testapp\troot\tsnapshot\t--interactive")
        assertEquals("ECHO: root\tsnapshot\t--interactive", result)
    }

    @Test
    fun unknownAppError() = runBlocking {
        val result = processCliCommand("nonexistent\troot\tsnapshot")
        assertTrue(result.startsWith("ERROR:"))
    }

    @Test
    fun emptyCommandError() = runBlocking {
        assertEquals("ERROR: empty command", processCliCommand(""))
    }

    @Test
    fun noCommandAfterAppId() = runBlocking {
        mockEchoApp()
        assertEquals("ERROR: no command after app ID", processCliCommand("testapp-jvm-x"))
    }

    @Test
    fun sequentialCommands() = runBlocking {
        mockEchoApp()
        assertEquals("ECHO: myButton\tclick", processCliCommand("testapp-jvm-x\tmyButton\tclick"))
        assertEquals("ECHO: email\tsetValue\ttest@example.com", processCliCommand("testapp-jvm-x\temail\tsetValue\ttest@example.com"))
    }

    // ===================== MCP Tool Tests =====================

    @Test
    fun mcpLs() = runBlocking {
        mockEchoApp()
        val result = executeLs()
        assertTrue(result.contains("testapp-jvm-x"))
    }

    @Test
    fun mcpLsEmpty() = runBlocking {
        assertEquals("(no apps connected)", executeLs())
    }

    @Test
    fun mcpDriveSingleCommand() = runBlocking {
        mockEchoApp()
        val result = executeDrive(McpDriveParams(app = "testapp", commands = listOf(listOf("root", "snapshot"))))
        assertFalse(result.isError)
        assertEquals(1, result.content.size)
        assertEquals("ECHO: root\tsnapshot", result.content[0].text)
    }

    @Test
    fun mcpDriveMultipleCommands() = runBlocking {
        mockEchoApp()
        val result = executeDrive(McpDriveParams(
            app = "testapp",
            commands = listOf(listOf("root", "click"), listOf("root", "snapshot"))
        ))
        assertFalse(result.isError)
        assertEquals(2, result.content.size)
        assertEquals("ECHO: root\tclick", result.content[0].text)
        assertEquals("ECHO: root\tsnapshot", result.content[1].text)
    }

    @Test
    fun mcpDriveWithDelay() = runBlocking {
        mockEchoApp()
        val start = System.currentTimeMillis()
        executeDrive(McpDriveParams(
            app = "testapp",
            commands = listOf(listOf("root", "click"), listOf("delay", "200"), listOf("root", "snapshot"))
        ))
        assertTrue(System.currentTimeMillis() - start >= 150, "Should have delayed ~200ms")
    }

    @Test
    fun mcpDriveStopsOnError() = runBlocking {
        mockEchoApp { cmd ->
            if (cmd.contains("fail")) "ERROR: simulated failure" else "ECHO: $cmd"
        }
        val result = executeDrive(McpDriveParams(
            app = "testapp",
            commands = listOf(listOf("root", "click"), listOf("root", "fail"), listOf("root", "snapshot"))
        ))
        assertTrue(result.isError)
        assertEquals(2, result.content.size)
        assertEquals("ECHO: root\tclick", result.content[0].text)
        assertEquals("ERROR: simulated failure", result.content[1].text)
    }

    @Test
    fun mcpDriveUnknownApp() = runBlocking {
        val result = executeDrive(McpDriveParams(app = "nonexistent", commands = listOf(listOf("root", "click"))))
        assertTrue(result.isError)
        assertTrue(result.content.first().text!!.startsWith("ERROR:"))
    }

    @Test
    fun mcpDriveScreenshotSavesToFile(): Unit = runBlocking {
        val pngBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAIAAACQd1PeAAAADElEQVR4nGP4//8/AAX+Av4N70a4AAAAAElFTkSuQmCC"
        mockEchoApp { pngBase64 }
        val result = executeDrive(McpDriveParams(
            app = "testapp",
            commands = listOf(listOf("root", "screenshot"))
        ))
        assertFalse(result.isError)
        assertEquals(1, result.content.size)
        val text = result.content[0].text!!
        assertTrue(text.startsWith("Screenshot saved: "))
        val path = text.removePrefix("Screenshot saved: ")
        val file = java.io.File(path)
        assertTrue(file.exists(), "Screenshot file should exist at $path")
        assertTrue(file.length() > 0, "Screenshot file should not be empty")
        file.delete()
    }
}
