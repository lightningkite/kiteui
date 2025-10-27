package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.AsyncImage
import com.lightningkite.kiteui.models.*
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Reactive
import org.jetbrains.skia.Image as SkiaImage

actual abstract class RawImageViewLike(context: RContext) : RView(context) {
    private lateinit var _source: ImageSource
    private lateinit var _description: String
    private lateinit var _scaleType: ImageScaleType

    // Expect requires final vals, so we provide backing fields and an init helper
    actual val source: ImageSource get() = _source
    actual val description: String get() = _description
    actual val scaleType: ImageScaleType get() = _scaleType

    protected fun initRawImageViewLike(source: ImageSource, description: String, scaleType: ImageScaleType) {
        this._source = source
        this._description = description
        this._scaleType = scaleType
    }

    actual abstract val state: Reactive<Unit>
}

actual class RawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType
) : RawImageViewLike(context) {
    init { initRawImageViewLike(source, description, scaleType) }
    actual override val state: Reactive<Unit> = Constant(Unit)

    @Composable
    override fun compose() {
        when (val source = source) {
            is ImageRemote -> {
                AsyncImage(
                    model = source.url,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageRaw -> {
                val painter = remember(source.data) {
                    BitmapPainter(SkiaImage.makeFromEncoded(source.data.data).asImageBitmap())
                }
                Image(
                    painter = painter,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageLocal -> {
                AsyncImage(
                    model = source.file.file,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageVector -> {
                val painter = source.toPainter()
                Image(
                    painter = painter,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageResource -> {
                val painter = remember(source.relativeUrl) {
                    val stream = this.javaClass.classLoader.getResourceAsStream(source.relativeUrl)
                    stream?.let {
                        BitmapPainter(SkiaImage.makeFromEncoded(it.readAllBytes()).asImageBitmap())
                    }
                }
                painter?.let {
                    Image(
                        painter = it,
                        contentDescription = description,
                        contentScale = scaleType.toCompose(),
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

actual class SizelessRawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType
) : RawImageViewLike(context) {
    init { initRawImageViewLike(source, description, scaleType) }
    actual override val state: Reactive<Unit> = Constant(Unit)

    @Composable
    override fun compose() {
        when (val source = source) {
            is ImageRemote -> {
                AsyncImage(
                    model = source.url,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageRaw -> {
                val painter = remember(source.data) {
                    BitmapPainter(SkiaImage.makeFromEncoded(source.data.data).asImageBitmap())
                }
                Image(
                    painter = painter,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageLocal -> {
                AsyncImage(
                    model = source.file.file,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageVector -> {
                val painter = source.toPainter()
                Image(
                    painter = painter,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = Modifier
                )
            }
            is ImageResource -> {
                val painter = remember(source.relativeUrl) {
                    val stream = this.javaClass.classLoader.getResourceAsStream(source.relativeUrl)
                    stream?.let {
                        BitmapPainter(SkiaImage.makeFromEncoded(it.readAllBytes()).asImageBitmap())
                    }
                }
                painter?.let {
                    Image(
                        painter = it,
                        contentDescription = description,
                        contentScale = scaleType.toCompose(),
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

actual class RawImageViewZoomable actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType
) : RawImageViewLike(context) {
    init { initRawImageViewLike(source, description, scaleType) }
    actual override val state: Reactive<Unit> = Constant(Unit)
    actual val zoomState: MutableReactiveValue<ZoomState> = Signal(ZoomState())

    @Composable
    override fun compose() {
        var scale by remember { mutableStateOf(1f) }
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }
        var rotation by remember { mutableStateOf(0f) }

        val imageModifier = Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, rotationDelta ->
                scale *= zoom
                offsetX += pan.x
                offsetY += pan.y
                rotation += rotationDelta
                zoomState.value = ZoomState(scale, offsetX, offsetY, rotation)
            }
        }.graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offsetX,
            translationY = offsetY,
            rotationZ = rotation
        )

        when (val source = source) {
            is ImageRemote -> {
                AsyncImage(
                    model = source.url,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = imageModifier
                )
            }
            is ImageRaw -> {
                val painter = remember(source.data) {
                    BitmapPainter(SkiaImage.makeFromEncoded(source.data.data).asImageBitmap())
                }
                Image(
                    painter = painter,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = imageModifier
                )
            }
            is ImageLocal -> {
                AsyncImage(
                    model = source.file.file,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = imageModifier
                )
            }
            is ImageVector -> {
                val painter = source.toPainter()
                Image(
                    painter = painter,
                    contentDescription = description,
                    contentScale = scaleType.toCompose(),
                    modifier = imageModifier
                )
            }
            is ImageResource -> {
                val painter = remember(source.relativeUrl) {
                    val stream = this.javaClass.classLoader.getResourceAsStream(source.relativeUrl)
                    stream?.let {
                        BitmapPainter(SkiaImage.makeFromEncoded(it.readAllBytes()).asImageBitmap())
                    }
                }
                painter?.let {
                    Image(
                        painter = it,
                        contentDescription = description,
                        contentScale = scaleType.toCompose(),
                        modifier = imageModifier
                    )
                }
            }
        }
    }
}

actual class ZoomState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f
)