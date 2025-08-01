package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

public actual class Video public actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "video"
    }
    public actual var source: VideoSource? = null
        set(value) {
            field = value
            when(value) {
                null -> native.attributes.src = ""
                is VideoRemote -> native.attributes.src = value.url
                is VideoRaw -> native.attributes.src = createObjectURL(value.data)
                is VideoResource -> native.attributes.src = context.basePath + value.relativeUrl
                is VideoLocal -> native.attributes.src = createObjectURL(value.file)
                else -> {}
            }
        }
    actual val time: MutableReactive<Double> = nativeTime
    actual val playing: MutableReactive<Boolean> = nativePlaying
    actual val volume: MutableReactive<Float> = nativeVolume
    actual var showControls: Boolean
        get() = native.attributes.controls != null
        set(value) {
            native.attributes.controls = value
            native.attributes.playsInline = !value
        }
    actual var loop: Boolean
        get() = native.attributes.loopBoolean != null
        set(value) { native.attributes.loopBoolean = value }
    public actual var scaleType: ImageScaleType = ImageScaleType.Fit
        set(value) {
            field = value
            native.classes.removeAll { it.startsWith("scaleType-") }
            native.classes.add("scaleType-$value")
        }
}
expect val Video.nativeTime: MutableReactive<Double>
expect val Video.nativePlaying: MutableReactive<Boolean>
expect val Video.nativeVolume: MutableReactive<Float>
