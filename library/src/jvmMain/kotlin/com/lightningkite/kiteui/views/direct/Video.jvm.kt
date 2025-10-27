package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactive

class Video(context: RContext) :
    RView(context) {
    var source: VideoSource?
        get() = TODO("Not yet implemented")
        set(value) {}
    val time: MutableReactive<Double>
        get() = TODO("Not yet implemented")
    val playing: MutableReactive<Boolean>
        get() = TODO("Not yet implemented")
    val volume: MutableReactive<Float>
        get() = TODO("Not yet implemented")
    var showControls: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    var loop: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    var scaleType: ImageScaleType
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}