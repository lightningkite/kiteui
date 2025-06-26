package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageResource
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ImageVector
import com.lightningkite.kiteui.models.vectorToSvgDataUrl
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.backgroundImage
import com.lightningkite.kiteui.views.backgroundPosition
import com.lightningkite.kiteui.views.backgroundRepeat
import com.lightningkite.kiteui.views.backgroundSize
import com.lightningkite.kiteui.views.position
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Property
import com.lightningkite.readable.RawReadable
import com.lightningkite.readable.Readable
import com.lightningkite.readable.ReadableState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue
import kotlin.js.JsName
import kotlin.setValue

actual abstract class RawImageViewLike(
    context: RContext,
    actual val source: ImageSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
) : RView(context) {
    actual abstract val state: Readable<Unit>
    val _state = RawReadable<Unit>()

    init {
        native.classes.add("scaleType-$scaleType")
    }

    protected fun ImageSource?.toUrl(): String? = when(val value = this) {
        null -> ""
        is ImageRemote -> value.url
        is ImageRaw -> createObjectURL(value.data)
        is ImageResource -> context.basePath + value.relativeUrl
        is ImageLocal -> createObjectURL(value.file)
        is ImageVector -> value.vectorToSvgDataUrl()
        else -> ""
    }
}

actual class RawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {

    init {
        native.tag = "img"
        native.classes.add("viewDraws")
    }
    actual override val state: Readable<Unit> = _state
    init { nativeLoad(source.toUrl()) }
}

actual class SizelessRawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {

    init {
        native.tag = "div"
        native.classes.add("viewDraws")
    }
    actual override val state: Readable<Unit> = _state
    init {
        native.style.backgroundImage = "url('${source.toUrl()}')"
        native.style.backgroundPosition = "center"
        native.style.backgroundRepeat = "no-repeat"
        native.style.backgroundSize = when(scaleType) {
            ImageScaleType.Fit -> "contain"
            ImageScaleType.Crop -> "cover"
            ImageScaleType.Stretch -> TODO("Not supported yet")
            ImageScaleType.NoScale -> "auto"
        }
        launch {
            delay(100L)
            _state.state = ReadableState(Unit)
        }
    }
}

actual class RawImageViewZoomable actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    init {
        native.tag = "img"
        native.classes.add("viewDraws")
    }
    actual override val state: Readable<Unit> = _state
    init { nativeLoad(source.toUrl()) }
    actual val zoomState: ImmediateWritable<ZoomState> = Property(Unit)
}

@JsName("createObjectURLBlob")
expect fun createObjectURL(blob: Blob): String

@JsName("createObjectURLFileReference")
expect fun createObjectURL(fileReference: FileReference): String

expect fun RawImageViewLike.nativeLoad(url: String?)

actual typealias ZoomState = Unit
