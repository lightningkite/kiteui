package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.InternalKiteUi
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
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Property
import com.lightningkite.signal.RawReadable
import com.lightningkite.signal.Readable
import kotlin.js.JsName

public actual abstract class RawImageViewLike(
    context: RContext,
    public actual val source: ImageSource,
    public actual val description: String,
    public actual val scaleType: ImageScaleType,
) : RView(context) {
    public actual abstract val state: Readable<Unit>
    public val _state: RawReadable<Unit> = RawReadable<Unit>()

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

public actual class RawImageView public actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    override val cannotBeCovered: Boolean get() = false

    init {
        native.tag = "img"
        native.classes.add("viewDraws")
    }
    public actual override val state: Readable<Unit> = _state
    init { nativeLoad(source.toUrl()) }
}

public actual class RawImageViewZoomable public actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    init {
        native.tag = "img"
        native.classes.add("viewDraws")
    }
    public actual override val state: Readable<Unit> = _state
    init { nativeLoad(source.toUrl()) }
    public actual val zoomState: ImmediateWritable<ZoomState> = Property(Unit)
}

@JsName("createObjectURLBlob")
public expect fun createObjectURL(blob: Blob): String

@JsName("createObjectURLFileReference")
public expect fun createObjectURL(fileReference: FileReference): String

@InternalKiteUi
public expect fun RawImageViewLike.nativeLoad(url: String?)

public actual typealias ZoomState = Unit
