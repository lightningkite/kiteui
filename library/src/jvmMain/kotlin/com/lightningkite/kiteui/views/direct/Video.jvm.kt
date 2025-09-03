package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactive

actual class Video actual constructor(context: RContext) :
    RView(context) {
    actual var source: VideoSource?
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val time: MutableReactive<Double>
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
    actual var scaleType: ImageScaleType
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        TODO("Not yet implemented")
    }
}