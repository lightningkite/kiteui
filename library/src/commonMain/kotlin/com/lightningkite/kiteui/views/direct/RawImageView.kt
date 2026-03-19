package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*

expect abstract class RawImageViewLike: NativeElement {
    val source: ImageSource
    val description: String
    val scaleType: ImageScaleType
    abstract val state: Reactive<Unit>
}

expect class RawImageView(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
}

expect class SizelessRawImageView(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
}

expect class RawImageViewZoomable(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
    val zoomState: MutableReactiveValue<ZoomState>
}
expect class ZoomState
