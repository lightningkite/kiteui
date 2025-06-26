package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Readable

expect abstract class RawImageViewLike: RView {
    val source: ImageSource
    val description: String
    val scaleType: ImageScaleType
    abstract val state: Readable<Unit>
}

expect class RawImageView(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Readable<Unit>
}

expect class SizelessRawImageView(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Readable<Unit>
}

expect class RawImageViewZoomable(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Readable<Unit>
    val zoomState: ImmediateWritable<ZoomState>
}
expect class ZoomState
