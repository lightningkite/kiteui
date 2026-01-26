package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.runOnUiThread
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.JComponent
import kotlin.concurrent.thread

actual abstract class RawImageViewLike constructor(
    context: RContext,
    actual val source: ImageSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
) : RView(context) {
    actual abstract val state: Reactive<Unit>
}

actual class RawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    actual override val state: Reactive<Unit> = Constant(Unit)

    override val native: ImagePanel = ImagePanel(scaleType, null)

    init {
        native.toolTipText = description
        loadImage()
    }

    private fun loadImage() {
        thread(start = true) {
            try {
                val image = when (val src = source) {
                    is ImageRaw -> loadFromByteArray(src.data.data)
                    is ImageRemote -> loadFromUrl(src.url)
                    is ImageLocal -> loadFromFile(src.file.file.absolutePath)
                    else -> null
                }
                runOnUiThread {
                    native.image = image
                    native.revalidate()
                    native.repaint()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFromByteArray(data: ByteArray): BufferedImage? {
        return ByteArrayInputStream(data).use { input ->
            ImageIO.read(input)
        }
    }

    private fun loadFromUrl(urlString: String): BufferedImage? {
        return ImageIO.read(URL(urlString))
    }

    private fun loadFromFile(path: String): BufferedImage? {
        return ImageIO.read(java.io.File(path))
    }
}

actual class SizelessRawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    actual override val state: Reactive<Unit> = Constant(Unit)

    override val native: ImagePanel = ImagePanel(scaleType, null, sizeless = true)

    init {
        native.toolTipText = description
        loadImage()
    }

    private fun loadImage() {
        thread(start = true) {
            try {
                val image = when (val src = source) {
                    is ImageRaw -> loadFromByteArray(src.data.data)
                    is ImageRemote -> loadFromUrl(src.url)
                    is ImageLocal -> loadFromFile(src.file.file.absolutePath)
                    else -> null
                }
                runOnUiThread {
                    native.image = image
                    native.revalidate()
                    native.repaint()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFromByteArray(data: ByteArray): BufferedImage? {
        return ByteArrayInputStream(data).use { input ->
            ImageIO.read(input)
        }
    }

    private fun loadFromUrl(urlString: String): BufferedImage? {
        return ImageIO.read(URL(urlString))
    }

    private fun loadFromFile(path: String): BufferedImage? {
        return ImageIO.read(java.io.File(path))
    }
}

actual class RawImageViewZoomable actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    actual override val state: Reactive<Unit> = Constant(Unit)

    private val _zoomState = Signal(ZoomState())
    actual val zoomState: MutableReactiveValue<ZoomState> = _zoomState

    override val native: ImagePanel = ImagePanel(scaleType, _zoomState)

    init {
        native.toolTipText = description
        loadImage()
        setupZoomControls()
    }

    private fun loadImage() {
        thread(start = true) {
            try {
                val image = when (val src = source) {
                    is ImageRaw -> loadFromByteArray(src.data.data)
                    is ImageRemote -> loadFromUrl(src.url)
                    is ImageLocal -> loadFromFile(src.file.file.absolutePath)
                    else -> null
                }
                runOnUiThread {
                    native.image = image
                    native.revalidate()
                    native.repaint()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFromByteArray(data: ByteArray): BufferedImage? {
        return ByteArrayInputStream(data).use { input ->
            ImageIO.read(input)
        }
    }

    private fun loadFromUrl(urlString: String): BufferedImage? {
        return ImageIO.read(URL(urlString))
    }

    private fun loadFromFile(path: String): BufferedImage? {
        return ImageIO.read(java.io.File(path))
    }

    private fun setupZoomControls() {
        var dragStartPoint: Point? = null

        native.addMouseWheelListener { e: MouseWheelEvent ->
            val state = _zoomState.value
            val scaleFactor = if (e.wheelRotation < 0) 1.1 else 0.9
            val newScale = (state.scale * scaleFactor).coerceIn(0.1, 10.0)

            // Zoom towards mouse position
            val mouseX = e.x.toDouble()
            val mouseY = e.y.toDouble()

            val newOffsetX = mouseX - (mouseX - state.offsetX) * (newScale / state.scale)
            val newOffsetY = mouseY - (mouseY - state.offsetY) * (newScale / state.scale)

            _zoomState.value = ZoomState(newScale, newOffsetX, newOffsetY)
            native.repaint()
        }

        native.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                dragStartPoint = e.point
                native.cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
            }

            override fun mouseReleased(e: MouseEvent) {
                dragStartPoint = null
                native.cursor = Cursor.getDefaultCursor()
            }
        })

        native.addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val start = dragStartPoint ?: return
                val state = _zoomState.value

                val dx = e.x - start.x
                val dy = e.y - start.y

                _zoomState.value = ZoomState(
                    state.scale,
                    state.offsetX + dx,
                    state.offsetY + dy
                )

                dragStartPoint = e.point
                native.repaint()
            }
        })
    }
}

actual class ZoomState(
    val scale: Double = 1.0,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0
)

class ImagePanel(
    private val scaleType: ImageScaleType,
    private val zoomState: Signal<ZoomState>?,
    private val sizeless: Boolean = false
) : JComponent() {
    var image: BufferedImage? = null
        set(value) {
            field = value
            revalidate()
            repaint()
        }

    // Make click-through if not zoomable, so images don't block clicks from parent Link/Button
    // Zoomable images need to receive mouse events for panning/zooming
    override fun contains(x: Int, y: Int): Boolean {
        return if (zoomState != null) super.contains(x, y) else false
    }

    override fun getPreferredSize(): java.awt.Dimension {
        if (sizeless) {
            return java.awt.Dimension(0, 0)
        }
        val img = image ?: return java.awt.Dimension(100, 100)
        return java.awt.Dimension(img.width, img.height)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val img = image ?: return

        val zoom = zoomState?.value
        if (zoom != null) {
            // Zoomable mode - apply zoom transform
            val transform = AffineTransform()
            transform.translate(zoom.offsetX, zoom.offsetY)
            transform.scale(zoom.scale, zoom.scale)
            g2d.drawImage(img, transform, null)
        } else {
            // Normal scaling mode
            val bounds = calculateImageBounds(img, width, height, scaleType)
            g2d.drawImage(img, bounds.x, bounds.y, bounds.width, bounds.height, null)
        }
    }

    private fun calculateImageBounds(
        img: BufferedImage,
        containerWidth: Int,
        containerHeight: Int,
        scaleType: ImageScaleType
    ): Rectangle {
        val imgWidth = img.width.toDouble()
        val imgHeight = img.height.toDouble()
        val imgAspect = imgWidth / imgHeight
        val containerAspect = containerWidth.toDouble() / containerHeight.toDouble()

        return when (scaleType) {
            ImageScaleType.Fit -> {
                // Scale to fit within bounds, preserving aspect ratio
                val scale = if (imgAspect > containerAspect) {
                    containerWidth / imgWidth
                } else {
                    containerHeight / imgHeight
                }
                val scaledWidth = (imgWidth * scale).toInt()
                val scaledHeight = (imgHeight * scale).toInt()
                val x = (containerWidth - scaledWidth) / 2
                val y = (containerHeight - scaledHeight) / 2
                Rectangle(x, y, scaledWidth, scaledHeight)
            }
            ImageScaleType.Crop -> {
                // Scale to fill bounds, preserving aspect ratio (may crop)
                val scale = if (imgAspect > containerAspect) {
                    containerHeight / imgHeight
                } else {
                    containerWidth / imgWidth
                }
                val scaledWidth = (imgWidth * scale).toInt()
                val scaledHeight = (imgHeight * scale).toInt()
                val x = (containerWidth - scaledWidth) / 2
                val y = (containerHeight - scaledHeight) / 2
                Rectangle(x, y, scaledWidth, scaledHeight)
            }
            ImageScaleType.Stretch -> {
                // Stretch to fill bounds, ignoring aspect ratio
                Rectangle(0, 0, containerWidth, containerHeight)
            }
            ImageScaleType.NoScale -> {
                // Show at original size, centered
                val x = (containerWidth - imgWidth.toInt()) / 2
                val y = (containerHeight - imgHeight.toInt()) / 2
                Rectangle(x, y, imgWidth.toInt(), imgHeight.toInt())
            }
        }
    }
}
