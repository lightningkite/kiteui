package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Reactive

actual abstract class RawImageViewLike(context: RContext) : RView(context) {
    actual val source: ImageSource
        get() = TODO("Not yet implemented")
    actual val description: String
        get() = TODO("Not yet implemented")
    actual val scaleType: ImageScaleType
        get() = TODO("Not yet implemented")
    actual abstract val state: Reactive<Unit>
}

actual class RawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType
) : RawImageViewLike(context) {
    actual override val state: Reactive<Unit>
        get() = TODO("Not yet implemented")

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}

actual class SizelessRawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType
) : RawImageViewLike(context) {
    actual override val state: Reactive<Unit>
        get() = TODO("Not yet implemented")

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}

actual class RawImageViewZoomable actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType
) : RawImageViewLike(context) {
    actual override val state: Reactive<Unit>
        get() = TODO("Not yet implemented")
    actual val zoomState: MutableReactiveValue<ZoomState>
        get() = TODO("Not yet implemented")

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}

actual class ZoomState