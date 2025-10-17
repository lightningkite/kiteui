package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*


expect class RawVideoView(
    context: RContext,
    source: VideoSource,
    description: String,
    scaleType: ImageScaleType,
    preloadHint: PreloadHint = PreloadHint.METADATA,
) : RView {
    val source: VideoSource
    val description: String
    val scaleType: ImageScaleType
    val preloadHint: PreloadHint
    val state: Reactive<Unit>
    val seekableTimeRanges: List<ClosedFloatingPointRange<Double>>

    val time: MutableReactive<Double>
    val playing: MutableReactive<Boolean>
    val volume: MutableReactive<Float>
    var showControls: Boolean
    var loop: Boolean
    val completedPlay: Listenable
}

enum class PreloadHint {
    NONE,
    METADATA,
    ALL
}