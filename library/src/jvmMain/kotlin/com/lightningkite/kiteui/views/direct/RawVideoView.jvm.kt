package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive

actual class RawVideoView actual constructor(
    context: RContext,
    source: VideoSource,
    description: String,
    scaleType: ImageScaleType,
    preloadHint: PreloadHint
) : RView(context) {
    actual val source: VideoSource
        get() = TODO("Not yet implemented")
    actual val description: String
        get() = TODO("Not yet implemented")
    actual val scaleType: ImageScaleType
        get() = TODO("Not yet implemented")
    actual val preloadHint: PreloadHint
        get() = TODO("Not yet implemented")
    actual val state: Reactive<Unit>
        get() = TODO("Not yet implemented")
    actual val seekableTimeRanges: List<ClosedFloatingPointRange<Double>>
        get() = TODO("Not yet implemented")
    actual val time: MutableReactive<Double>
        get() = TODO("Not yet implemented")
    actual val sourceDuration: Reactive<Double?>
        get() = TODO("Not yet implemented")
    actual val playing: MutableReactive<Boolean>
        get() = TODO("Not yet implemented")
    actual val volume: MutableReactive<Float>
        get() = TODO("Not yet implemented")
    actual var showControls: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var loop: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val completedPlay: Listenable
        get() = TODO("Not yet implemented")

    @Composable
    override fun compose(): Unit = TODO()
}