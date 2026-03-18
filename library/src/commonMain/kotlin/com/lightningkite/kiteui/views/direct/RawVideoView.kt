package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.*
import kotlin.time.Duration


expect class RawVideoView(
    context: ElementContext,
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

    @Deprecated("Use currentTime instead")
    val time: MutableReactive<Double>
    val currentTime: MutableReactive<Duration>

    /**
     * The duration of the video in seconds.
     */
    val sourceDuration: Reactive<Double?>
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