package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*
import kotlin.time.Duration


public expect class RawVideoView(
    context: ElementContext,
    source: VideoSource,
    description: String,
    scaleType: ImageScaleType,
    preloadHint: PreloadHint = PreloadHint.METADATA,
) : NativeElement {
    public val source: VideoSource
    public val description: String
    public val scaleType: ImageScaleType
    public val preloadHint: PreloadHint
    public val state: Reactive<Unit>
    public val seekableTimeRanges: List<ClosedFloatingPointRange<Double>>

    @Deprecated("Use currentTime instead")
    public val time: MutableReactive<Double>
    public val currentTime: MutableReactive<Duration>

    /**
     * The duration of the video in seconds.
     */
    public val sourceDuration: Reactive<Double?>
    public val playing: MutableReactive<Boolean>
    public val volume: MutableReactive<Float>
    public var showControls: Boolean
    public var loop: Boolean
    public val completedPlay: Listenable
}

public enum class PreloadHint {
    NONE,
    METADATA,
    ALL
}