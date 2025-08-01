package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.RContext

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import kotlin.jvm.JvmInline
import kotlin.contracts.*


public expect class Video(context: RContext) : RView {

    public var source: VideoSource?
    public val time: Writable<Double>
    public val playing: Writable<Boolean>
    public val volume: Writable<Float>
    public var showControls: Boolean
    public var loop: Boolean
    public var scaleType: ImageScaleType
}