package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*

public expect abstract class RawImageViewLike: NativeElement {
    public val source: ImageSource
    public val description: String
    public val scaleType: ImageScaleType
    public abstract val state: Reactive<Unit>
}

public expect class RawImageView(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
}

public expect class SizelessRawImageView(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
}

public expect class RawImageViewZoomable(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike {
    override val state: Reactive<Unit>
    public val zoomState: MutableReactiveValue<ZoomState>
}
public expect class ZoomState
