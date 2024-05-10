//package com.lightningkite.kiteui.views.direct
//
//import ViewWriter
//import com.lightningkite.kiteui.await
//import com.lightningkite.kiteui.models.ImageLocal
//import com.lightningkite.kiteui.models.ImageRaw
//import com.lightningkite.kiteui.views.*
//import com.lightningkite.kiteui.views.canvas.clear
//import kotlinx.browser.window
//import org.khronos.webgl.ArrayBuffer
//import org.w3c.dom.*
//import org.w3c.dom.events.Event
//import org.w3c.dom.events.MouseEvent
//import org.w3c.dom.pointerevents.PointerEvent
//import org.w3c.files.Blob
//import org.w3c.files.FileReader
//import kotlin.coroutines.resume
//import kotlin.coroutines.resumeWithException
//import kotlin.coroutines.suspendCoroutine
//import kotlin.js.Promise
//import kotlin.math.*
//
//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NImageCrop = HTMLCanvasElement
//
//actual class ImageCrop actual constructor(actual override val native: NImageCrop) : RView<NImageCrop> {
//    actual var source: ImageLocal? = null
//        set(value) {
//            field = value
//            launch {
//                bitmap = source?.let { window.createImageBitmap(it.file).await() }
//                context.draw()
//            }
//        }
//
//    actual var aspectRatio: Pair<Int, Int>? = null
//        set(value) {
//            field = value
//
//            value?.let {
//                // For any aspect ratio, there are two options that preserve one of the current dimensions
//                val widthOfFirstOption = cropRegion.height * (value.first / value.second)
//                val heightOfSecondOption = cropRegion.width * (value.second / value.first)
//
//                // Pick the one that results in making a dimension smaller
//                if (widthOfFirstOption < cropRegion.width) {
//                    cropRegion = cropRegion.copy(width = widthOfFirstOption)
//                } else {
//                    cropRegion = cropRegion.copy(height = heightOfSecondOption)
//                }
//            }
//        }
//
//    private val context = (native.getContext("2d") as CanvasRenderingContext2D).apply {
//        imageSmoothingQuality = ImageSmoothingQuality.HIGH
//    }
//    private var bitmap: ImageBitmap? = null
//
//    init {
//        native.style.setProperty("touch-action", "none")
//
//        native.addEventListener("mousemove", ::handleMouseMove)
//        native.addEventListener("pointerdown", ::handleTouchStart)
//        native.addEventListener("pointerup", ::handleTouchEndOrCancel)
//        native.addEventListener("pointercancel", ::handleTouchEndOrCancel)
//        native.addEventListener("pointermove", ::handleTouchMove)
//
//        ResizeObserver(::sizeCanvas).observe(native)
//
//        sizeCanvas()
//    }
//
//    data class Rect(val x: Double, val y: Double, val width: Double, val height: Double) {
//        fun translate(dx: Double, dy: Double) = copy(
//            x = x + dx,
//            y = y + dy
//        )
//
//        enum class Corner {
//            TopLeft,
//            TopRight,
//            BottomLeft,
//            BottomRight
//        }
//
//        fun pointAtCorner(corner: Corner): Pair<Double, Double> = (x + (corner.ordinal % 2 * width) to
//                y + (corner.ordinal / 2 * height))
//
//        fun contains(point: Pair<Double, Double>) =
//            point.first in rangeBetween(x, x + width) && point.second in rangeBetween(y, y + height)
//
//        fun contains(friend: Rect): Boolean {
//            for (corner in Corner.entries) {
//                if (!contains(friend.pointAtCorner(corner))) return false
//            }
//            return true
//        }
//
//        private fun rangeBetween(a: Double, b: Double) = min(a, b) .. max(a, b)
//
//        private fun Double.trim(lower: Double, higher: Double) = max(min(this, higher), lower)
//    }
//
//    private val THUMB_RADIUS = 10.0
//    private var cropRegion = Rect(40.0, 40.0, 150.0, 80.0)
//    private var imageRegion = Rect(0.0, 0.0, 0.0, 0.0)
//    private var fitHorizontal = true    // If false, fit image vertically
//
//    private fun CanvasRenderingContext2D.draw() {
//        clear()
//
//        bitmap?.let {
//            val imageAspectRatio: Double = it.width.toDouble() / it.height
//            val canvasAspectRatio: Double = native.width.toDouble() / native.height
//            fitHorizontal = imageAspectRatio > canvasAspectRatio
//
//            if (fitHorizontal) {
//                val width = native.width.toDouble()
//                imageRegion = imageRegion.copy(
//                    width = width,
//                    height = width / imageAspectRatio
//                )
//            } else {
//                val height = native.height.toDouble()
//                imageRegion = imageRegion.copy(
//                    height = height,
//                    width = height * imageAspectRatio
//                )
//            }
//
//            drawImage(it, 0.0, 0.0, imageRegion.width, imageRegion.height)
//        }
//
//        drawThumbs()
//    }
//
//    private fun CanvasRenderingContext2D.drawThumbs() = cropRegion.run {
//        strokeStyle = "white"
//        lineWidth = 2.0
//        beginPath()
//        rect(x, y, width, height)
//        stroke()
//
//        Rect.Corner.entries.map(cropRegion::pointAtCorner).forEach {
//            fillStyle = "white"
//            strokeStyle = "cornflowerblue"
//            lineWidth = 3.0
//            beginPath()
//            arc(it.first, it.second, THUMB_RADIUS, 0.0, 2 * PI)
//            fill()
//            stroke()
//        }
//    }
//
//    private fun sizeCanvas(a: Array<ResizeObserverEntry>? = null, b: ResizeObserver? = null) {
//        native.width = native.offsetWidth
//        native.height = native.offsetHeight
//    }
//
//    private fun handleMouseMove(event: Event) {
//        event as MouseEvent
//        val pointerCoordinates = event.offsetX to event.offsetY
//
//        event.preventDefault()
//        event.stopPropagation()
//
//        if (dragThumbHandlers.isNotEmpty() || moveCropRegionHandlers.isNotEmpty()) { native.style.cursor = "grabbing"; return }
//
//        if (withinThumbTouchTarget(pointerCoordinates) != null || cropRegion.contains(pointerCoordinates)) {
//            native.style.cursor = "grab"
//        } else {
//            native.style.cursor = "default"
//        }
//    }
//
//    private fun handleThumbMove(thumb: Rect.Corner, x: Double, y: Double) {
//        val cornerPoint = cropRegion.pointAtCorner(thumb)
//
//        var deltaWidth = (x - cornerPoint.first) * if (thumb == Rect.Corner.TopLeft || thumb == Rect.Corner.BottomLeft) -1 else 1
//        var deltaHeight = (y - cornerPoint.second) * if (thumb == Rect.Corner.TopLeft || thumb == Rect.Corner.TopRight) -1 else 1
//
//        aspectRatio?.let { aspectRatio ->
//            if (deltaWidth > deltaHeight) {
//                deltaHeight = deltaWidth * (aspectRatio.second / aspectRatio.first)
//            } else {
//                deltaWidth = deltaHeight * (aspectRatio.first / aspectRatio.second)
//            }
//        }
//
//        val newCropRegion = Rect(
//            x = if (thumb == Rect.Corner.TopLeft || thumb == Rect.Corner.BottomLeft) cropRegion.x - deltaWidth else cropRegion.x,
//            y = if (thumb == Rect.Corner.TopLeft || thumb == Rect.Corner.TopRight) cropRegion.y - deltaHeight else cropRegion.y,
//            width = cropRegion.width + deltaWidth,
//            height = cropRegion.height + deltaHeight
//        )
//
//        if (imageRegion.contains(newCropRegion)) {
//            cropRegion = newCropRegion
//            context.draw()
//        }
//    }
//
//    private fun handleRegionDrag(transformationOrigin: Pair<Double, Double>, x: Double, y: Double): Pair<Double, Double> {
//        val newCropRegion = cropRegion.translate(
//            dx = x - transformationOrigin.first,
//            dy = y - transformationOrigin.second
//        )
//
//        if (imageRegion.contains(newCropRegion)) {
//            cropRegion = newCropRegion
//            context.draw()
//        }
//
//        return x to y
//    }
//
//    private fun withinThumbTouchTarget(point: Pair<Double, Double>): Rect.Corner? {
//        for (corner in Rect.Corner.entries) {
//            val thumb = cropRegion.pointAtCorner(corner)
//
//            val distance = sqrt((point.first - thumb.first).pow(2) + (point.second - thumb.second).pow(2))
//            if (distance <= THUMB_RADIUS) {
//                return corner
//            }
//        }
//        return null
//    }
//
//    private val dragThumbHandlers = mutableMapOf<Int, Rect.Corner>()
//    private val moveCropRegionHandlers = mutableMapOf<Int, Pair<Double, Double>>()
//
//    private fun handleTouchStart(event: Event) {
//        event as PointerEvent
//        val pointerCoordinates = event.offsetX to event.offsetY
//
//        withinThumbTouchTarget(pointerCoordinates)?.let {
//            dragThumbHandlers[event.pointerId] = it
//            return
//        }
//
//        if (cropRegion.contains(pointerCoordinates)) {
//            moveCropRegionHandlers[event.pointerId] = pointerCoordinates
//            return
//        }
//    }
//
//    private fun handleTouchEndOrCancel(event: Event) {
//        event as PointerEvent
//        dragThumbHandlers.remove(event.pointerId)
//        moveCropRegionHandlers.remove(event.pointerId)
//    }
//
//    private fun handleTouchMove(event: Event) {
//        event as PointerEvent
//
//        dragThumbHandlers[event.pointerId]?.let {
//            handleThumbMove(it, event.offsetX, event.offsetY)
//            return
//        }
//
//        moveCropRegionHandlers[event.pointerId]?.let {
//            moveCropRegionHandlers[event.pointerId] = handleRegionDrag(it, event.offsetX, event.offsetY)
//            return
//        }
//    }
//
//    actual suspend fun crop(): ImageRaw? {
//        val bitmap = bitmap ?: return null
//        val scale: Double = if (fitHorizontal) {
//            native.width.toDouble() / bitmap.width
//        } else {
//            native.height.toDouble() / bitmap.height
//        }
//
//        val resultWidth = (cropRegion.width.absoluteValue / scale).toInt()
//        val resultHeight = (cropRegion.height.absoluteValue / scale).toInt()
//
//        val cropCanvas = OffscreenCanvas(resultWidth, resultHeight)
//        val cropContext = cropCanvas.getContext("2d") as OffscreenCanvasRenderingContext2D
//
//        cropContext.drawImage(
//            bitmap,
//            sx = cropRegion.x / scale,
//            sy = cropRegion.y / scale,
//            sw = cropRegion.width / scale,
//            sh = cropRegion.height / scale,
//            dx = 0.0,
//            dy = 0.0,
//            dw = resultWidth.toDouble(),
//            dh = resultHeight.toDouble()
//        )
//
//        return ImageRaw(cropCanvas.convertToBlob().await())
//    }
//}
//
//external class OffscreenCanvas(width: Int, height: Int) {
//    var width: Int
//    var height: Int
//    fun getContext(contextType: String, vararg arguments: Any): Any?
//    fun convertToBlob(): Promise<Blob>
//}
//
//external class OffscreenCanvasRenderingContext2D {
//    fun drawImage(image: CanvasImageSource, sx: Double, sy: Double, sw: Double, sh: Double, dx: Double, dy: Double, dw: Double, dh: Double)
//}
//
//suspend fun FileReader.readAsArrayBufferSync(blob: Blob): ArrayBuffer = suspendCoroutine { continuation ->
//    onload = {
//        continuation.resume(result as ArrayBuffer)
//    }
//    onerror = {
//        continuation.resumeWithException(Exception())
//    }
//
//    readAsArrayBuffer(blob)
//}
//
//@ViewDsl
//actual inline fun ViewWriter.imageCropActual(crossinline setup: ImageCrop.() -> Unit): Unit =
//    element<HTMLCanvasElement>("canvas") {
//        setup(ImageCrop(this))
//    }
