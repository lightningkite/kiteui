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
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.alt
import com.lightningkite.kiteui.views.backgroundImage
import com.lightningkite.kiteui.views.loading
import com.lightningkite.kiteui.views.backgroundPosition
import com.lightningkite.kiteui.views.backgroundRepeat
import com.lightningkite.kiteui.views.backgroundSize
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.*
import kotlin.js.JsName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

public actual abstract class RawImageViewLike(
    context: ElementContext,
    public actual val source: ImageSource,
    public actual val description: String,
    public actual val scaleType: ImageScaleType,
) : NativeElement(context) {
    public actual abstract val state: Reactive<Unit>
    internal val _state: RawReactive<Unit> = RawReactive<Unit>()

    // by Claude - track blob URLs created by createObjectURL so we can revoke them to prevent memory leaks
    private var currentBlobUrl: String? = null

    init {
        native.classes.add("scaleType-$scaleType")
        // by Claude - revoke blob URL when view is shut down
        onRemove {
            currentBlobUrl?.let { revokeObjectURL(it) }
            currentBlobUrl = null
        }
    }

    protected fun ImageSource?.toUrl(): String? {
        // by Claude - revoke previous blob URL before creating a new one
        currentBlobUrl?.let { revokeObjectURL(it) }
        currentBlobUrl = null
        return when(val value = this) {
            null -> ""
            is ImageRemote -> value.url
            is ImageRaw -> createObjectURL(value.data).also { currentBlobUrl = it }
            is ImageResource -> context.basePath + value.relativeUrl
            is ImageLocal -> createObjectURL(value.file).also { currentBlobUrl = it }
            is ImageVector -> value.vectorToSvgDataUrl()
            else -> ""
        }
    }
}

public actual class RawImageView actual constructor(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {

    init {
        native.tag = "img"
        native.classes.add("viewDraws")
        native.attributes.alt = description  // by Claude - SEO alt attribute
        native.attributes.loading = "lazy"  // by Claude - lazy loading for performance
    }
    actual override val state: Reactive<Unit> = _state
    init { nativeLoad(source.toUrl()) }
}

public actual class SizelessRawImageView actual constructor(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {

    init {
        native.tag = "div"
        native.classes.add("viewDraws")
        native.setAttribute("role", "img")
        native.setAttribute("aria-label", description)
    }
    actual override val state: Reactive<Unit> = _state
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
            _state.state = ReactiveState(Unit)
        }
    }
}

public actual class RawImageViewZoomable actual constructor(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    init {
        native.tag = "img"
        native.classes.add("viewDraws")
        native.attributes.alt = description  // by Claude - SEO alt attribute
        native.attributes.loading = "lazy"  // by Claude - lazy loading for performance
    }
    actual override val state: Reactive<Unit> = _state
    init { nativeLoad(source.toUrl()) }
    public actual val zoomState: MutableReactiveValue<ZoomState> = Signal(Unit)
}

@JsName("createObjectURLBlob")
public expect fun createObjectURL(blob: Blob): String

@JsName("createObjectURLFileReference")
public expect fun createObjectURL(fileReference: FileReference): String

// by Claude - revoke blob URLs to prevent memory leaks
public expect fun revokeObjectURL(url: String)

public expect fun RawImageViewLike.nativeLoad(url: String?)

public actual typealias ZoomState = Unit
