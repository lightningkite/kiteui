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

public expect abstract class RawImageViewLike: RView {
    public val source: ImageSource
    public val description: String
    public val scaleType: ImageScaleType
    public abstract val state: Reactive<Unit>
}

public expect class RawImageView(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    public override val state: Reactive<Unit>
}

public expect class SizelessRawImageView(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    public override val state: Reactive<Unit>
}

public expect class RawImageViewZoomable(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    public override val state: Reactive<Unit>
    public val zoomState: MutableReactiveValue<ZoomState>
}
public expect class ZoomState
