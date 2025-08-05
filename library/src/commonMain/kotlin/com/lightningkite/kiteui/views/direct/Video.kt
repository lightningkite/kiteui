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


public expect class Video(context: RContext) : RView {

    public var source: VideoSource?
    public val time: MutableReactive<Double>
    public val playing: MutableReactive<Boolean>
    public val volume: MutableReactive<Float>
    public var showControls: Boolean
    public var loop: Boolean
    public var scaleType: ImageScaleType
}