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
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Property
import com.lightningkite.readable.RawReadable
import com.lightningkite.readable.Readable
import kotlin.js.JsName

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
    resizeWhenLoaded: Boolean,
) : RawImageViewLike(context, source, description, scaleType) {
    override val cannotBeCovered: Boolean get() = false

    init {
        native.tag = "img"
        native.classes.add("viewDraws")
    }
    actual override val state: Readable<Unit> = _state
    init { nativeLoad(source.toUrl()) }
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
