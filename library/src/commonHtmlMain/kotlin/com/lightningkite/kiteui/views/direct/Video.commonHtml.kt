package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@InternalKiteUi
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
    public actual val time: MutableReactive<Double> = nativeTime
    public actual val playing: MutableReactive<Boolean> = nativePlaying
    public actual val volume: MutableReactive<Float> = nativeVolume
    public actual var showControls: Boolean
        get() = native.attributes.controls != null
        set(value) {
            native.attributes.controls = value
            native.attributes.playsInline = !value
        }
    public actual var loop: Boolean
        get() = native.attributes.loopBoolean != null
        set(value) { native.attributes.loopBoolean = value }
    public actual var scaleType: ImageScaleType = ImageScaleType.Fit
        set(value) {
            field = value
            native.classes.removeAll { it.startsWith("scaleType-") }
            native.classes.add("scaleType-$value")
        }
}
public expect val Video.nativeTime: MutableReactive<Double>
public expect val Video.nativePlaying: MutableReactive<Boolean>
public expect val Video.nativeVolume: MutableReactive<Float>
