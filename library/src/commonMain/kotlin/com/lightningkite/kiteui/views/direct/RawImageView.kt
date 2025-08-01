package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Readable

public expect abstract class RawImageViewLike: RView {
    public val source: ImageSource
    public val description: String
    public val scaleType: ImageScaleType
    public abstract val state: Readable<Unit>
}

public expect class RawImageView(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    public override val state: Readable<Unit>
}

public expect class RawImageViewZoomable(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    public override val state: Readable<Unit>
    public val zoomState: ImmediateWritable<ZoomState>
}
public expect class ZoomState
