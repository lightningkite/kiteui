package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.contracts.*
import kotlin.jvm.JvmInline


expect class Video(context: RContext) : RView {

    var source: VideoSource?
    val time: MutableReactive<Double>
    val playing: MutableReactive<Boolean>
    val volume: MutableReactive<Float>
    var showControls: Boolean
    var loop: Boolean
    var scaleType: ImageScaleType
}