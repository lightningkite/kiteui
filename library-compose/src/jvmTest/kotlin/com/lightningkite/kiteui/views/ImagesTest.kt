package com.lightningkite.kiteui.views

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.lightningkite.kiteui.createFileReferenceFromBytes
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.toBlob
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.net.InetSocketAddress
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ImagesTest {
    // 40x20: left half red, right half blue, with a 10x10 green square in the top-left corner.
    private val png = javaClass.classLoader.getResourceAsStream("common/images/test-pattern.png")!!.use { it.readBytes() }
    private val vector = ImageVector(
        40.px, 20.px, viewBoxWidth = 40, viewBoxHeight = 20,
        paths = listOf(
            ImageVector.Path(fillColor = Color(1f, 0.86f, 0f, 0f), path = "M0,0 L20,0 L20,20 L0,20 Z"),
            ImageVector.Path(fillColor = Color(1f, 0f, 0f, 0.86f), path = "M20,0 L40,0 L40,20 L20,20 Z"),
            ImageVector.Path(fillColor = Color(1f, 0f, 0.78f, 0f), path = "M0,0 L10,0 L10,10 L0,10 Z"),
        ),
    )

    private val requests = mutableListOf<String>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            synchronized(requests) { requests += exchange.requestURI.toString() }
            if (exchange.requestURI.path == "/missing.png") {
                exchange.sendResponseHeaders(404, -1)
            } else {
                exchange.responseHeaders.add("Content-Type", "image/png")
                exchange.sendResponseHeaders(200, png.size.toLong())
                exchange.responseBody.use { it.write(png) }
            }
            exchange.close()
        }
        start()
    }
    private val baseUrl = "http://127.0.0.1:${server.address.port}"

    @AfterTest
    fun stopServer() = server.stop(0)

    private fun ComposeUiTest.waitForLoad(views: List<RawImageViewLike>) {
        waitUntil(timeoutMillis = 10_000) { views.all { it.state.state.ready } }
        views.forEach { check(it.state.state.success) { "${it.description} failed: ${it.state.state}" } }
        waitForIdle()
    }

    // The main colour at a point within [box], as 'R', 'G', 'B', 'W' (white), 'K' (black), or '-' for anything else.
    private fun BufferedImage.colorAt(root: KiteUiRoot, box: DpRect, x: Dp, y: Dp): Char {
        val rgb = getRGB(((box.left + x).value * root.density).toInt(), ((box.top + y).value * root.density).toInt())
        val r = rgb shr 16 and 0xFF
        val g = rgb shr 8 and 0xFF
        val b = rgb and 0xFF
        return when {
            r > 150 && g < 80 && b < 80 -> 'R'
            g > 150 && r < 80 && b < 80 -> 'G'
            b > 150 && r < 80 && g < 80 -> 'B'
            r > 235 && g > 235 && b > 235 -> 'W'
            r < 60 && g < 60 && b < 60 -> 'K'
            else -> '-'
        }
    }

    @Test
    fun everySourceDrawsWithEveryScaleType() = runKiteUiTest {
        val sources = listOf(
            "raw" to ImageRaw(png.toBlob("image/png")),
            "resource" to ImageResource("common/images/test-pattern.png"),
            "local" to ImageLocal(createFileReferenceFromBytes(png, "image/png", "pattern.png")),
            "remote" to ImageRemote("$baseUrl/pattern.png"),
            "vector" to vector,
        )
        val views = mutableListOf<RawImageViewLike>()
        val root = showKiteUi {
            col {
                for ((name, source) in sources) row {
                    for (scaleType in ImageScaleType.entries) {
                        sizeConstraints(width = 60.px, height = 60.px).rawImage(source, "$name $scaleType", scaleType) { views += this }
                    }
                }
            }
        }
        waitForLoad(views)
        val shot = saveScreenshot(root, "image-sources")

        for ((name, _) in sources) {
            fun box(scaleType: ImageScaleType) = onNodeWithContentDescription("$name $scaleType").getUnclippedBoundsInRoot()
            // 40x20 into 60x60: stretched to fill, so the green corner is 15x30.
            val stretch = box(ImageScaleType.Stretch)
            assertEquals("GRB", "${shot.colorAt(root, stretch, 5.dp, 5.dp)}${shot.colorAt(root, stretch, 20.dp, 50.dp)}${shot.colorAt(root, stretch, 50.dp, 50.dp)}", "$name Stretch")
            // Fit: 60x30, centered vertically.
            val fit = box(ImageScaleType.Fit)
            assertEquals("KGBK", "${shot.colorAt(root, fit, 5.dp, 5.dp)}${shot.colorAt(root, fit, 5.dp, 20.dp)}${shot.colorAt(root, fit, 50.dp, 40.dp)}${shot.colorAt(root, fit, 50.dp, 55.dp)}", "$name Fit")
            // Crop: 120x60, centered horizontally, so only the middle 20 image pixels show and no green.
            val crop = box(ImageScaleType.Crop)
            assertEquals("RB", "${shot.colorAt(root, crop, 5.dp, 5.dp)}${shot.colorAt(root, crop, 55.dp, 5.dp)}", "$name Crop")
            // NoScale: 40x20 at one dp per pixel, centered.
            val noScale = box(ImageScaleType.NoScale)
            assertEquals("KGB", "${shot.colorAt(root, noScale, 5.dp, 5.dp)}${shot.colorAt(root, noScale, 12.dp, 22.dp)}${shot.colorAt(root, noScale, 45.dp, 35.dp)}", "$name NoScale")
        }
    }

    @Test
    fun imagesSizeLikeAnImgElement() = runKiteUiTest {
        val views = mutableListOf<RawImageViewLike>()
        val resource = ImageResource("common/images/test-pattern.png")
        val root = showKiteUi {
            col {
                centered.rawImage(resource, "natural") { views += this }
                rawImage(resource, "stretched") { views += this }
                centered.rawImage(vector, "vector natural") { views += this }
                centered.rawImageUnsized(resource, "sizeless") { views += this }
            }
        }
        waitForLoad(views)
        saveScreenshot(root, "image-sizes")

        val natural = onNodeWithContentDescription("natural").getUnclippedBoundsInRoot()
        assertEquals(40.dp, natural.width)
        assertEquals(20.dp, natural.height)
        val stretched = onNodeWithContentDescription("stretched").getUnclippedBoundsInRoot()
        assertTrue(stretched.width > 100.dp, "stretched to the column's width, got ${stretched.width}")
        assertTrue(abs((stretched.height - stretched.width / 2).value) <= 1f, "keeps the aspect ratio, got ${stretched.width} x ${stretched.height}")
        val vectorNatural = onNodeWithContentDescription("vector natural").getUnclippedBoundsInRoot()
        assertEquals(40.dp, vectorNatural.width)
        assertEquals(20.dp, vectorNatural.height)
        val sizeless = onNodeWithContentDescription("sizeless").getUnclippedBoundsInRoot()
        assertEquals(0.dp, sizeless.width)
    }

    @Test
    fun remoteImagesAreCachedPerStrategy() = runKiteUiTest {
        val current = Signal<ImageRemote?>(null)
        var view: RawImageView? = null
        var shown: ImageRemote? = null
        showKiteUi {
            swapping(current = { current() }) { source ->
                shown = source
                view = null
                if (source != null) rawImage(source, "remote") { view = this }
            }
        }
        // Swaps run on the Swing thread, which waitForIdle doesn't track, so wait for each one to happen.
        fun load(source: ImageRemote) {
            // Through null, since swapping to an equal source does nothing.
            current.value = null
            waitUntil { shown == null }
            current.value = source
            waitUntil(timeoutMillis = 10_000) { shown == source && view?.state?.state?.ready == true }
        }
        fun requestCount() = synchronized(requests) { requests.size }

        load(ImageRemote("$baseUrl/full.png?sig=1", UrlCacheStrategy.Full))
        load(ImageRemote("$baseUrl/full.png?sig=1", UrlCacheStrategy.Full))
        assertEquals(1, requestCount(), "Full caches the exact URL")
        load(ImageRemote("$baseUrl/full.png?sig=2", UrlCacheStrategy.Full))
        assertEquals(2, requestCount(), "Full keys on the query too")

        load(ImageRemote("$baseUrl/path.png?sig=1", UrlCacheStrategy.PathOnly))
        load(ImageRemote("$baseUrl/path.png?sig=2", UrlCacheStrategy.PathOnly))
        assertEquals(3, requestCount(), "PathOnly ignores the query")

        load(ImageRemote("$baseUrl/none.png", UrlCacheStrategy.None))
        load(ImageRemote("$baseUrl/none.png", UrlCacheStrategy.None))
        assertEquals(5, requestCount(), "None always downloads")

        load(ImageRemote("$baseUrl/missing.png", UrlCacheStrategy.None))
        assertTrue(view!!.state.state.exception != null, "A 404 fails the load")
    }

    @Test
    fun zoomableImageZoomsAndPans() = runKiteUiTest {
        var zoomable: RawImageViewZoomable? = null
        val root = showKiteUi {
            col {
                sizeConstraints(width = 200.px, height = 100.px).rawImageZoomable(ImageResource("common/images/test-pattern.png"), "zoomable") {
                    zoomable = this
                }
            }
        }
        val view = zoomable!!
        waitForLoad(listOf(view))
        val node = onNodeWithContentDescription("zoomable")

        node.performMouseInput { moveTo(center); scroll(-1f) }
        waitForIdle()
        val zoomed = view.zoomState.value
        assertTrue(zoomed.scale > 1f, "the wheel zooms in, got ${zoomed.scale}")

        node.performTouchInput { swipe(center, center + Offset(30f, 0f)) }
        waitForIdle()
        assertTrue(view.zoomState.value.offsetX != zoomed.offsetX, "dragging pans, got ${view.zoomState.value.offsetX}")

        // On the main thread, where the view applies it.
        runBlocking(Dispatchers.Main) { view.zoomState.value = ZoomState(scale = 3f) }
        waitForIdle()
        val shot = saveScreenshot(root, "image-zoomed")
        // At 3x from the top-left corner, the green square covers the left 150 of the view.
        val box = node.getUnclippedBoundsInRoot()
        assertEquals('G', shot.colorAt(root, box, 100.dp, 50.dp))
        assertEquals('R', shot.colorAt(root, box, 190.dp, 50.dp))
    }

    // The web's WheelEvent deltas are pixels: 100 a wheel notch in Chrome on Windows and Linux.
    @Test
    fun canvasWheelDeltasArePixels() = runKiteUiTest {
        var wheel = 0.0
        showKiteUi {
            canvas {
                delegate = object : CanvasDelegate() {
                    override fun onWheel(x: Double, y: Double, z: Double): Boolean {
                        wheel += y
                        return true
                    }
                }
            }
        }
        onRoot().performMouseInput { moveTo(center); scroll(1f) }
        waitForIdle()
        assertTrue(wheel >= 10.0, "a notch down should be many pixels, got $wheel")
    }

    // Graph sizes its lines and labels with canvasUnits.
    @Test
    fun canvasUnitsAreTheCanvasPixels() = runKiteUiTest {
        var canvasWidth = 0.0
        val root = runBlocking(Dispatchers.Main) {
            KiteUiRoot("kiteui-test", { testTheme }) {
                col {
                    sizeConstraints(width = 100.px, height = 50.px).canvas {
                        delegate = object : CanvasDelegate() {
                            override fun onResize(width: Double, height: Double) {
                                canvasWidth = width
                            }
                        }
                    }
                }
            }
        }
        setContent { CompositionLocalProvider(LocalDensity provides Density(2f)) { root.Content() } }
        waitForIdle()
        assertEquals(200.0, canvasWidth)
        assertEquals(canvasWidth, 100.px.canvasUnits)
    }
    // Both GIFs are 20x20, red for 300ms and then blue for 300ms; one loops forever and the other plays once.
    @Test
    fun gifsAnimate() = runKiteUiTest {
        lateinit var view: RawImageView
        val root = showKiteUi {
            col { sizeConstraints(width = 20.px, height = 20.px).rawImage(ImageResource("common/images/red-blue-looping.gif"), "gif") { view = this } }
        }
        waitForLoad(listOf(view))
        fun color() = saveScreenshot(root, "gif").colorAt(root, onNodeWithContentDescription("gif").getUnclippedBoundsInRoot(), 10.dp, 10.dp)
        waitUntil(timeoutMillis = 5_000) { color() == 'B' }
        waitUntil(timeoutMillis = 5_000) { color() == 'R' }
    }

    @Test
    fun gifsPlayOnlyWhileSeenAndRepeatAsOftenAsTheySay() = runKiteUiTest {
        lateinit var view: RawImageView
        var setVisible: (Boolean) -> Unit = {}
        val root = showKiteUi {
            col {
                frame {
                    setVisible = { visible = it }
                    visible = false
                    sizeConstraints(width = 20.px, height = 20.px).rawImage(ImageResource("common/images/red-blue-once.gif"), "gif") { view = this }
                }
            }
        }
        waitForLoad(listOf(view))
        mainClock.advanceTimeBy(1_000)
        runBlocking(Dispatchers.Main) { setVisible(true) }
        fun color() = saveScreenshot(root, "gif-once").colorAt(root, onNodeWithContentDescription("gif").getUnclippedBoundsInRoot(), 10.dp, 10.dp)
        assertEquals('R', color(), "it didn't play while invisible")
        waitUntil(timeoutMillis = 5_000) { color() == 'B' }
        mainClock.advanceTimeBy(1_000)
        assertEquals('B', color(), "it played once")
    }

    @Test
    fun canvasDrawsTheHtmlCanvasApi() = runKiteUiTest {
        var draws = 0
        val pointerDowns = mutableListOf<Pair<Double, Double>>()
        val measurements = mutableMapOf<String, Any>()
        val delegate = object : CanvasDelegate() {
            override fun draw(context: DrawingContext2D) = with(context) {
                draws++
                fillPaint = Color.white
                fillRect(0.0, 0.0, width, height)
                clearRect(width - 20, 0.0, 20.0, 20.0)

                // Fill and stroke a path.
                beginPath()
                moveTo(20.0, 80.0)
                lineTo(60.0, 10.0)
                lineTo(100.0, 80.0)
                closePath()
                fillPaint = Color.red
                fill()
                strokePaint = Color.black
                lineWidth = 4.0
                stroke()
                measurements["inTriangle"] = isPointInPath(60.0, 50.0)
                measurements["outsideTriangle"] = isPointInPath(5.0, 5.0)

                // A gradient round rect with a dashed outline.
                fillPaint = LinearGradient(listOf(GradientStop(0f, Color.blue), GradientStop(1f, Color.green)), x0 = 120.0, y0 = 0.0, x1 = 200.0, y1 = 0.0)
                beginPath()
                roundRect(120.0, 10.0, 80.0, 70.0, 15.0)
                fill()
                setLineDash(listOf(8.0, 4.0))
                lineWidth = 2.0
                stroke()
                setLineDash(emptyList())

                // A radial gradient circle, and a ring by even-odd.
                fillPaint = RadialGradient(listOf(GradientStop(0f, Color.white), GradientStop(1f, Color.blue)), cx = 240.0, cy = 45.0, radius = 30.0)
                beginPath()
                appendArc(240.0, 45.0, 30.0, Angle.zero, Angle.circle, false)
                fill()
                fillPaint = Color.green
                beginPath()
                appendArc(320.0, 45.0, 30.0, Angle.zero, Angle.circle, false)
                appendArc(320.0, 45.0, 15.0, Angle.zero, Angle.circle, false)
                fillEvenOdd()

                // A pie slice, which needs arcs joined to the path.
                fillPaint = Color(1f, 0.9f, 0.6f, 0f)
                beginPath()
                moveTo(400.0, 45.0)
                appendArc(400.0, 45.0, 30.0, Angle.zero, Angle.quarterTurn, false)
                closePath()
                fill()

                // Transforms, including one changed part-way through a path.
                save()
                translate(60.0, 140.0)
                rotate(PI / 6)
                fillPaint = Color.blue
                fillRect(-20.0, -20.0, 40.0, 40.0)
                measurements["transform"] = getTransform()
                restore()
                measurements["restored"] = getTransform()
                strokePaint = Color.black
                lineWidth = 3.0
                beginPath()
                moveTo(110.0, 110.0)
                translate(40.0, 0.0)
                lineTo(110.0, 170.0)
                setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                stroke()

                // A rotated ellipse, bezier curves.
                fillPaint = Color(0.6f, 0.6f, 0f, 0.6f)
                beginPath()
                ellipse(220.0, 140.0, 40.0, 20.0, PI / 4, 0.0, 2 * PI)
                fill()
                strokePaint = Color(1f, 0.4f, 0.2f, 0.7f)
                beginPath()
                moveTo(270.0, 170.0)
                quadraticCurveTo(300.0, 100.0, 330.0, 170.0)
                bezierCurveTo(350.0, 100.0, 380.0, 200.0, 400.0, 120.0)
                stroke()

                // Clipping.
                save()
                beginPath()
                rect(20.0, 200.0, 60.0, 60.0)
                clip()
                fillPaint = Color.green
                beginPath()
                appendArc(50.0, 230.0, 50.0, Angle.zero, Angle.circle, false)
                fill()
                restore()

                // A shadow, alpha and compositing.
                save()
                shadowColorValue = Color.black.copy(alpha = 0.6f)
                shadowBlur = 10.0
                shadowOffsetX = 6.0
                shadowOffsetY = 6.0
                fillPaint = Color.blue
                fillRect(110.0, 200.0, 50.0, 50.0)
                restore()
                // The transform doesn't scale a shadow, as in HTML.
                save()
                translate(10.0, 272.0)
                scale(4.0, 4.0)
                shadowColorValue = Color.black
                shadowOffsetX = 20.0
                fillPaint = Color.green
                fillRect(0.0, 0.0, 4.0, 4.0)
                restore()
                globalAlpha = 0.5
                fillPaint = Color.red
                fillRect(180.0, 200.0, 50.0, 50.0)
                globalAlpha = 1.0
                globalCompositeOperation = "multiply"
                fillPaint = Color.yellow
                fillRect(200.0, 220.0, 50.0, 50.0)
                globalCompositeOperation = "source-over"

                // Text.
                font(24.0, FontAndStyle(weight = 700))
                fillPaint = Color.black
                textAlign(TextAlign.left)
                drawText("Hello", 270.0, 230.0)
                measurements["textWidth"] = measureText("Hello").width
                textAlign(TextAlign.center)
                strokePaint = Color.blue
                lineWidth = 1.0
                drawOutlinedText("Outlined", 360.0, 270.0)
            }

            override fun onPointerDown(id: Int, x: Double, y: Double, width: Double, height: Double): Boolean {
                pointerDowns += x to y
                return true
            }
        }
        var canvas: Canvas? = null
        val root = showKiteUi {
            col {
                sizeConstraints(width = 440.px, height = 290.px).canvas {
                    this.delegate = delegate
                    canvas = this
                }
            }
        }
        val shot = saveScreenshot(root, "canvas")
        val bounds = canvas!!.screenRectangle()!!
        val box = DpRect(bounds.left.toFloat().dp, bounds.top.toFloat().dp, bounds.right.toFloat().dp, bounds.bottom.toFloat().dp)
        // Canvas coordinates are pixels.
        fun at(x: Double, y: Double) = shot.colorAt(root, box, (x / root.density).toFloat().dp, (y / root.density).toFloat().dp)

        assertEquals('R', at(60.0, 55.0), "filled path")
        assertEquals('W', at(300.0, 10.0), "white background")
        assertTrue(at(430.0, 5.0) != 'W', "clearRect leaves no white")
        assertEquals('W', at(320.0, 45.0), "even-odd hole")
        assertEquals('G', at(320.0, 22.0), "even-odd ring")
        assertEquals('W', at(296.0, 21.0), "the ring is round")
        assertEquals('K', at(130.0, 140.0), "a line whose transform changed part-way")
        assertEquals('G', at(25.0, 205.0), "inside the clip")
        assertEquals('W', at(10.0, 230.0), "outside the clip")
        assertEquals('G', at(18.0, 280.0), "a scaled square")
        assertEquals('K', at(38.0, 280.0), "its shadow, 20 canvas pixels along")
        assertEquals('W', at(98.0, 280.0), "not 20 scaled pixels along")
        assertEquals(true, measurements["inTriangle"])
        assertEquals(false, measurements["outsideTriangle"])
        val transform = measurements["transform"] as TransformMatrix
        assertEquals(cos(PI / 6), transform.a, 1e-9)
        assertEquals(60.0, transform.e, 1e-9)
        assertEquals(TransformMatrix.identity, measurements["restored"])
        assertTrue((measurements["textWidth"] as Double) > 30.0)

        val drawsBefore = draws
        delegate.invalidate()
        waitForIdle()
        assertTrue(draws > drawsBefore, "invalidate() redraws")

        onRoot().performTouchInput { click(Offset((box.left.value + 100) * root.density, (box.top.value + 50) * root.density)) }
        waitForIdle()
        assertEquals(listOf(100.0 * root.density to 50.0 * root.density), pointerDowns)
    }

    @Test
    fun canvasRadialGradientsStartAtTheirFocus() = runKiteUiTest {
        val delegate = object : CanvasDelegate() {
            override fun draw(context: DrawingContext2D) = with(context) {
                fillPaint = RadialGradient(
                    listOf(GradientStop(0f, Color.white), GradientStop(1f, Color.blue)),
                    cx = 100.0, cy = 50.0, radius = 50.0, fx = 60.0, fy = 50.0,
                )
                fillRect(0.0, 0.0, width, height)
            }
        }
        var canvas: Canvas? = null
        val root = showKiteUi {
            col {
                sizeConstraints(width = 200.px, height = 100.px).canvas {
                    this.delegate = delegate
                    canvas = this
                }
            }
        }
        val shot = saveScreenshot(root, "canvas-focal-gradient")
        val bounds = canvas!!.screenRectangle()!!
        val box = DpRect(bounds.left.toFloat().dp, bounds.top.toFloat().dp, bounds.right.toFloat().dp, bounds.bottom.toFloat().dp)
        fun at(x: Double, y: Double) = shot.colorAt(root, box, (x / root.density).toFloat().dp, (y / root.density).toFloat().dp)

        // Centered, this point would be 80% of the way out; from the focus it's the first stop.
        assertEquals('W', at(60.0, 50.0), "the focus")
        assertEquals('B', at(140.0, 50.0), "near the edge")
    }
}
