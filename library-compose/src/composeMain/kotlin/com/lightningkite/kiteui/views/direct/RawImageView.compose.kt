package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.times
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.toByteArray
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toBrush
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.vector.ImageVector as ComposeImageVector

public actual abstract class RawImageViewLike(
    context: ElementContext,
    public actual val source: ImageSource,
    public actual val description: String,
    public actual val scaleType: ImageScaleType,
) : NativeElement(context) {
    public actual abstract val state: Reactive<Unit>
    internal val loadState = RawReactive<Unit>()
    private var bitmap: ImageBitmap? by mutableStateOf(null)

    init {
        if (source is ImageVector) loadState.state = ReactiveState(Unit)
        else launch {
            loadState.state = try {
                bitmap = loadImage(source)
                ReactiveState(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReactiveState.exception(e)
            }
        }
    }

    // The image, or null until it loads.  Its intrinsic size is its natural size.
    @Composable
    protected fun painter(): Painter? {
        val source = source
        if (source is ImageVector) return rememberVectorPainter(remember(source) { source.toCompose() })
        val bitmap = bitmap ?: return null
        val density = LocalDensity.current.density
        return remember(bitmap, density) { DpBitmapPainter(bitmap, density) }
    }

    // Not Modifier.paint, which also changes the constraints the element is measured with.
    protected fun Modifier.drawImage(painter: Painter?): Modifier = semantics {
        contentDescription = description
        role = Role.Image
    }.drawBehind {
        if (painter == null) return@drawBehind
        val contentScale = when (scaleType) {
            ImageScaleType.Fit -> ContentScale.Fit
            ImageScaleType.Crop -> ContentScale.Crop
            ImageScaleType.Stretch -> ContentScale.FillBounds
            ImageScaleType.NoScale -> ContentScale.None
        }
        val drawnSize = painter.intrinsicSize * contentScale.computeScaleFactor(painter.intrinsicSize, size)
        translate((size.width - drawnSize.width) / 2, (size.height - drawnSize.height) / 2) {
            with(painter) { draw(drawnSize) }
        }
    }
}

public actual class RawImageView actual constructor(context: ElementContext, source: ImageSource, description: String, scaleType: ImageScaleType) : RawImageViewLike(context, source, description, scaleType) {
    actual override val state: Reactive<Unit> = loadState

    @Composable
    override fun Content(modifier: Modifier) {
        val painter = painter()
        Layout(modifier.clipToBounds().drawImage(painter), NaturalSize(painter))
    }
}

// Takes no space of its own, like the web's background image: it fills whatever its parent gives it.
public actual class SizelessRawImageView actual constructor(context: ElementContext, source: ImageSource, description: String, scaleType: ImageScaleType) : RawImageViewLike(context, source, description, scaleType) {
    actual override val state: Reactive<Unit> = loadState

    @Composable
    override fun Content(modifier: Modifier) {
        Spacer(modifier.clipToBounds().drawImage(painter()))
    }
}

public actual class RawImageViewZoomable actual constructor(context: ElementContext, source: ImageSource, description: String, scaleType: ImageScaleType) : RawImageViewLike(context, source, description, scaleType) {
    actual override val state: Reactive<Unit> = loadState
    public actual val zoomState: MutableReactiveValue<ZoomState> = Signal(ZoomState())
    private var zoom by mutableStateOf(ZoomState())

    init {
        reactive { zoom = zoomState() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val painter = painter()
        var m = modifier.clipToBounds()
        if (takesInput) m = m
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, factor, _ -> zoomState.value = zoom.zoomed(centroid, pan, factor, size) }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val change = event.changes.first()
                        zoomState.value = zoom.zoomed(change.position, Offset.Zero, 1.25f.pow(-change.scrollDelta.y), size)
                        change.consume()
                    }
                }
            }
        Layout(
            m
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offsetX
                    translationY = zoom.offsetY
                }
                .drawImage(painter),
            NaturalSize(painter),
        )
    }
}

/** How far a [RawImageViewZoomable] is zoomed in, and where the zoomed image's top-left corner is, in pixels. */
public actual class ZoomState(public val scale: Float = 1f, public val offsetX: Float = 0f, public val offsetY: Float = 0f)

// Zooms about focus and pans, keeping the view covered by the image, between 1x and 3x as PhotoView does on Android.
private fun ZoomState.zoomed(focus: Offset, pan: Offset, factor: Float, size: IntSize): ZoomState {
    val newScale = (scale * factor).coerceIn(1f, 3f)
    val applied = newScale / scale
    return ZoomState(
        scale = newScale,
        offsetX = (focus.x - (focus.x - offsetX) * applied + pan.x).coerceIn(size.width * (1 - newScale), 0f),
        offsetY = (focus.y - (focus.y - offsetY) * applied + pan.y).coerceIn(size.height * (1 - newScale), 0f),
    )
}

// Sizes like the web's <img>: its natural size, shrunk to fit if need be.  When the parent fixes one axis,
// the other follows the image's aspect ratio.
private class NaturalSize(val painter: Painter?) : MeasurePolicy {
    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val natural = painter?.intrinsicSize
        val (width, height) = when {
            natural == null || (constraints.hasFixedWidth && constraints.hasFixedHeight) -> constraints.minWidth to constraints.minHeight
            constraints.hasFixedWidth -> constraints.maxWidth to constraints.constrainHeight((constraints.maxWidth * natural.height / natural.width).roundToInt())
            constraints.hasFixedHeight -> constraints.constrainWidth((constraints.maxHeight * natural.width / natural.height).roundToInt()) to constraints.maxHeight
            else -> {
                val scale = min(1f, min(constraints.maxWidth / natural.width, constraints.maxHeight / natural.height))
                constraints.constrainWidth((natural.width * scale).roundToInt()) to constraints.constrainHeight((natural.height * scale).roundToInt())
            }
        }
        return layout(width, height) {}
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int {
        val natural = painter?.intrinsicSize ?: return 0
        return if (height == Constraints.Infinity) natural.width.roundToInt() else (height * natural.width / natural.height).roundToInt()
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int {
        val natural = painter?.intrinsicSize ?: return 0
        return if (width == Constraints.Infinity) natural.height.roundToInt() else (width * natural.height / natural.width).roundToInt()
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        maxIntrinsicWidth(measurables, height)

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        maxIntrinsicHeight(measurables, width)
}

// Draws one image pixel per dp, as the web draws one per CSS px.
private class DpBitmapPainter(val bitmap: ImageBitmap, density: Float) : Painter() {
    override val intrinsicSize: Size = Size(bitmap.width * density, bitmap.height * density)
    override fun DrawScope.onDraw() {
        drawImage(bitmap, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
    }
}

// Like the web's SVG rendering of an ImageVector.
private fun ImageVector.toCompose(): ComposeImageVector = ComposeImageVector.Builder(
    defaultWidth = width.toDp(),
    defaultHeight = height.toDp(),
    viewportWidth = viewBoxWidth.toFloat(),
    viewportHeight = viewBoxHeight.toFloat(),
).group(translationX = -viewBoxMinX.toFloat(), translationY = -viewBoxMinY.toFloat()) {
    for (p in paths) addPath(
        pathData = PathParser().parsePathString(p.path).toNodes(),
        pathFillType = PathFillType.EvenOdd,
        fill = p.fillColor?.toBrush(),
        stroke = p.strokeColor?.let { SolidColor(it.toCompose()) },
        strokeLineWidth = (p.strokeWidth ?: 0.0).toFloat(),
        strokeLineCap = when (p.strokeCap) {
            Icon.StrokeLineCap.Round -> StrokeCap.Round
            Icon.StrokeLineCap.Square -> StrokeCap.Square
            Icon.StrokeLineCap.Butt, null -> StrokeCap.Butt
        },
    )
}.build()

// Decoded remote images by ImageRemote.cacheKey, as Glide's memory cache keeps them on Android.  The web relies on
// the browser's HTTP cache instead.  Least recently used first; only touched on the main thread.
private val remoteImages = LinkedHashMap<String, ImageBitmap>()
private const val remoteImagesMaxBytes = 128L * 1024 * 1024

private suspend fun loadImage(source: ImageSource): ImageBitmap {
    val cacheKey = (source as? ImageRemote)?.takeIf { it.cacheStrategy != UrlCacheStrategy.None }?.cacheKey
    if (cacheKey != null) remoteImages.remove(cacheKey)?.let {
        remoteImages[cacheKey] = it
        return it
    }
    val bytes = when (source) {
        is ImageRemote -> {
            val response = fetch(source.url)
            if (!response.ok) throw IllegalStateException("Failed to load image ${source.url}: HTTP ${response.status}")
            response.blob().toByteArray()
        }
        is ImageRaw -> source.data.toByteArray()
        is ImageLocal -> source.file.readBytes()
        is ImageResource -> source.readBytes()
        else -> throw IllegalArgumentException("$source is drawn, not loaded")
    }
    val image = withContext(Dispatchers.Default) { decodeImage(bytes) }
    if (cacheKey != null) {
        remoteImages[cacheKey] = image
        fun ImageBitmap.bytes() = width.toLong() * height * 4
        var total = remoteImages.values.sumOf { it.bytes() }
        while (total > remoteImagesMaxBytes && remoteImages.size > 1) {
            val oldest = remoteImages.keys.first()
            total -= remoteImages.remove(oldest)!!.bytes()
        }
    }
    return image
}

internal expect fun decodeImage(bytes: ByteArray): ImageBitmap
internal expect suspend fun FileReference.readBytes(): ByteArray
internal expect suspend fun ImageResource.readBytes(): ByteArray
