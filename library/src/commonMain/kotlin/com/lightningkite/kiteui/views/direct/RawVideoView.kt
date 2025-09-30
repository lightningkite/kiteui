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
) : RView {
    val source: VideoSource
    val description: String
    val scaleType: ImageScaleType
    val state: Reactive<Unit>

    val time: MutableReactive<Double>
    val playing: MutableReactive<Boolean>
    val volume: MutableReactive<Float>
    var showControls: Boolean
    var loop: Boolean
}
