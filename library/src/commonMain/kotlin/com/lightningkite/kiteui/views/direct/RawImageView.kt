package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

expect abstract class RawImageViewLike: RView {
    val source: ImageSource
    val description: String
    val scaleType: ImageScaleType
    abstract val state: Reactive<Unit>
}

expect class RawImageView(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
}

expect class SizelessRawImageView(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
}

expect class RawImageViewZoomable(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
    val zoomState: MutableReactiveValue<ZoomState>
}
expect class ZoomState
