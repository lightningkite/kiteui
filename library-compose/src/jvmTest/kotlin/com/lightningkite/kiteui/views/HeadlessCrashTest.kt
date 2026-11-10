package com.lightningkite.kiteui.views

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HeadlessCrashTest {
    object ThrowingApp {
        @JvmStatic
        fun main(args: Array<String>) {
            // Like the AI driver's connection in a real app, this alone keeps the process up.
            thread { Thread.sleep(Long.MAX_VALUE) }
            kiteUiHeadless("kiteui-test", { testTheme }) {
                write(object : NativeElement(context) {
                    @Composable
                    override fun Content(modifier: Modifier) = Box(modifier.size(10.dp).drawBehind { error("Drawing failed") })
                }) {}
            }
        }
    }

    @Test
    fun aRenderCrashEndsTheProcess() {
        val output = File.createTempFile("headless-crash", ".log")
        val java = File(System.getProperty("java.home"), "bin/java").path
        val process = ProcessBuilder(
            java,
            "-cp", System.getProperty("java.class.path"),
            "-Dkiteui.dataDirectory=${System.getProperty("kiteui.dataDirectory")}",
            ThrowingApp::class.java.name,
        ).redirectErrorStream(true).redirectOutput(output).start()
        val exited = process.waitFor(60, TimeUnit.SECONDS)
        if (!exited) process.destroyForcibly()
        assertTrue(exited, "Still running after the render crash")
        assertNotEquals(0, process.exitValue())
        assertContains(output.readText(), "Drawing failed")
    }
}
