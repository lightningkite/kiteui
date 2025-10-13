package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

actual class RawVideoView actual constructor(
    context: RContext,
    actual val source: VideoSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
) : RView(context) {
    val _state = RawReactive<Unit>()
    actual val state: Reactive<Unit> = _state

    init {
        native.tag = "video"
        native.classes.add("viewDraws")
        native.classes.add("scaleType-$scaleType")
        // Set initial attributes
        when (val value = source) {
            is VideoRemote -> native.attributes.src = value.url
            is VideoRaw -> native.attributes.src = createObjectURL(value.data)
            is VideoResource -> native.attributes.src = context.basePath + value.relativeUrl
            is VideoLocal -> native.attributes.src = createObjectURL(value.file)
            else -> native.attributes.src = ""
        }
        nativeLoad(native.attributes.src)
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

    actual val completedPlay: Listenable = native.vevent("ended")
}

expect val RawVideoView.nativeTime: MutableReactive<Double>
expect val RawVideoView.nativePlaying: MutableReactive<Boolean>
expect val RawVideoView.nativeVolume: MutableReactive<Float>

expect fun RawVideoView.nativeLoad(url: String?)
