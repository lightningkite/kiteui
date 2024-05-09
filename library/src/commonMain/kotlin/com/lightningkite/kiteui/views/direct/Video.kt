package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


expect class Video(context: RContext) : RView {

    var source: VideoSource?
    val time: Writable<Double>
    val playing: Writable<Boolean>
    val volume: Writable<Float>
    var showControls: Boolean
    var loop: Boolean
    var scaleType: ImageScaleType
}