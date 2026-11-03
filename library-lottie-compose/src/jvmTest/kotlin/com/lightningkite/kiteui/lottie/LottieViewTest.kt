package com.lightningkite.kiteui.lottie

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.kiteui.lottie.models.LottieRaw
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.lottie.views.direct.LottieView
import com.lightningkite.kiteui.lottie.views.direct.lottie
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.KiteUiRoot
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.reactive.core.Reactive
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import java.net.InetSocketAddress
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class, ExperimentalKiteUi::class, Untested::class)
class LottieViewTest {
    private val theme = Theme.flat2("test", 0.6.turns)

    // One second, 200x200: a 40x40 red square sliding linearly from x=40 to x=160 at y=100, under a still
    // blue circle 40 across at (100, 40).
    private val json = """
        {
          "v": "5.5.7", "fr": 30, "ip": 0, "op": 30, "w": 200, "h": 200,
          "layers": [
            {
              "ty": 4, "nm": "circle", "ip": 0, "op": 60, "st": 0,
              "ks": { "p": {"a": 0, "k": [100, 40]} },
              "shapes": [{ "ty": "gr", "it": [
                { "ty": "el", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [40, 40]} },
                { "ty": "fl", "c": {"a": 0, "k": [0, 0, 1, 1]}, "o": {"a": 0, "k": 100} },
                { "ty": "tr" }
              ]}]
            },
            {
              "ty": 4, "nm": "square", "ip": 0, "op": 60, "st": 0,
              "ks": { "p": {"a": 1, "k": [
                {"t": 0, "s": [40, 100], "e": [160, 100], "o": {"x": [0], "y": [0]}, "i": {"x": [1], "y": [1]}},
                {"t": 30, "s": [160, 100]}
              ]}},
              "shapes": [{ "ty": "gr", "it": [
                { "ty": "rc", "p": {"a": 0, "k": [0, 0]}, "s": {"a": 0, "k": [40, 40]}, "r": {"a": 0, "k": 0} },
                { "ty": "fl", "c": {"a": 0, "k": [1, 0, 0, 1]}, "o": {"a": 0, "k": 100} },
                { "ty": "tr" }
              ]}]
            }
          ]
        }
    """.trimIndent()

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            if (exchange.requestURI.path == "/missing.json") {
                exchange.sendResponseHeaders(404, -1)
            } else {
                val bytes = json.encodeToByteArray()
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            exchange.close()
        }
        start()
    }
    private val baseUrl = "http://127.0.0.1:${server.address.port}"

    @AfterTest
    fun stopServer() = server.stop(0)

    private fun ComposeUiTest.show(app: ViewWriter.() -> Unit): KiteUiRoot {
        val root = runBlocking(Dispatchers.Main) { KiteUiRoot("kiteui-lottie-test", { theme }, app) }
        setContent { root.Content() }
        waitForIdle()
        return root
    }

    private fun ComposeUiTest.waitForLoad(view: LottieView) {
        waitUntil(timeoutMillis = 10_000) { view.state.state.ready }
        check(view.state.state.success) { "${view.source} failed: ${view.state.state}" }
        waitForIdle()
    }

    private fun <T> Reactive<T>.now(): T = state.get()

    private fun ComposeUiTest.screenshot(root: KiteUiRoot, name: String): BufferedImage {
        waitForIdle()
        val image = runBlocking { root.screenshot() }.toAwtImage()
        val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
        ImageIO.write(image, "png", File(out, "$name.png"))
        return image
    }

    // 'R', 'B', or '-' for anything else, at a point within [box].
    private fun BufferedImage.colorAt(root: KiteUiRoot, box: DpRect, x: Dp, y: Dp): Char {
        val rgb = getRGB(((box.left + x).value * root.density).toInt(), ((box.top + y).value * root.density).toInt())
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        return when {
            r > 200 && g < 60 && b < 60 -> 'R'
            b > 200 && r < 60 && g < 60 -> 'B'
            else -> '-'
        }
    }

    @Test
    fun loadsSeeksAndDraws() = runComposeUiTest {
        lateinit var view: LottieView
        // 200x200 fit into 100x100, so every animation coordinate halves.
        val root = show {
            col { sizeConstraints(width = 100.px, height = 100.px).lottie(LottieRaw(json), "slider") { view = this; autoPlay = false } }
        }
        waitForLoad(view)
        assertEquals(1.seconds, view.duration.now())
        assertFalse(view.playing.now())
        assertEquals(0f, view.progress.now())
        val box = onNodeWithContentDescription("slider").getUnclippedBoundsInRoot()
        assertEquals(100.dp, box.right - box.left)

        // Square from (10, 40) to (30, 60) at the start; circle centered on (50, 20).
        val start = screenshot(root, "lottie-start")
        assertEquals("RB-", "${start.colorAt(root, box, 20.dp, 50.dp)}${start.colorAt(root, box, 50.dp, 20.dp)}${start.colorAt(root, box, 80.dp, 50.dp)}")

        runBlocking(Dispatchers.Main) { view.seekToFrame(15) }
        assertEquals(0.5f, view.progress.now())
        val middle = screenshot(root, "lottie-middle")
        assertEquals("-R-", "${middle.colorAt(root, box, 20.dp, 50.dp)}${middle.colorAt(root, box, 50.dp, 50.dp)}${middle.colorAt(root, box, 80.dp, 50.dp)}")

        runBlocking(Dispatchers.Main) { view.progress.set(1f) }
        val end = screenshot(root, "lottie-end")
        assertEquals("-RB", "${end.colorAt(root, box, 20.dp, 50.dp)}${end.colorAt(root, box, 80.dp, 50.dp)}${end.colorAt(root, box, 50.dp, 20.dp)}")
        root.close()
    }

    @Test
    fun playsCompletesAndLoops() = runComposeUiTest {
        lateinit var view: LottieView
        var completions = 0
        val root = show {
            col {
                centered.lottie(LottieRaw(json), "slider") {
                    view = this
                    loop = false
                    completedPlay.addListener { completions++ }
                }
            }
        }
        waitForLoad(view)
        assertEquals(200.dp, onNodeWithContentDescription("slider").getUnclippedBoundsInRoot().let { it.right - it.left })
        // autoPlay is on by default.
        waitUntil(timeoutMillis = 5_000) { view.progress.now() > 0.2f }
        assertTrue(view.playing.now())
        waitUntil(timeoutMillis = 5_000) { completions == 1 }
        assertFalse(view.playing.now())
        assertEquals(1f, view.progress.now())

        // Played again from the end, it starts over; looping, it wraps around instead of completing.
        runBlocking(Dispatchers.Main) {
            view.loop = true
            view.play()
        }
        waitUntil(timeoutMillis = 5_000) { view.progress.now() > 0.7f }
        waitUntil(timeoutMillis = 5_000) { view.progress.now() < 0.3f }
        assertEquals(1, completions)

        runBlocking(Dispatchers.Main) { view.playing.set(false) }
        val pausedAt = view.progress.now()
        Thread.sleep(200)
        assertEquals(pausedAt, view.progress.now())

        runBlocking(Dispatchers.Main) { view.stop() }
        assertEquals(0f, view.progress.now())
        assertFalse(view.playing.now())
        root.close()
    }

    @Test
    fun remoteSourcesLoadAndFail() = runComposeUiTest {
        lateinit var remote: LottieView
        lateinit var missing: LottieView
        val root = show {
            col {
                lottie(LottieRemote("$baseUrl/slider.json"), "remote") {
                    remote = this
                    autoPlay = false
                    // Asked to play before it has loaded, it plays once it has.
                    play()
                }
                lottie("$baseUrl/missing.json", "missing") { missing = this }
            }
        }
        waitForLoad(remote)
        waitUntil(timeoutMillis = 5_000) { remote.progress.now() > 0f }
        assertTrue(remote.playing.now())
        waitUntil(timeoutMillis = 10_000) { missing.state.state.ready }
        assertFalse(missing.state.state.success)
        assertEquals(null, missing.duration.now())
        root.close()
    }
}
