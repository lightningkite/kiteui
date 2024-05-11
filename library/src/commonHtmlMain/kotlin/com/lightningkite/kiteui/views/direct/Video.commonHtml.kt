package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*

actual class Video actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "video"
    }
    actual inline var source: VideoSource?
        get() = TODO()
        set(value) {
            when(value) {
                null -> native.attributes.src = ""
                is VideoRemote -> native.attributes.src = value.url
                is VideoRaw -> native.attributes.src = createObjectURL(value.data)
                is VideoResource -> native.attributes.src = context.basePath + value.relativeUrl
                is VideoLocal -> native.attributes.src = createObjectURL(value.file)
                else -> {}
            }
        }
    actual val time: Writable<Double> = nativeTime
    actual val playing: Writable<Boolean> = nativePlaying
    actual val volume: Writable<Float> = nativeVolume
    actual var showControls: Boolean
        get() = native.attributes.controls != null
        set(value) { native.attributes.controls = value }
    actual var loop: Boolean
        get() = native.attributes.loopBoolean != null
        set(value) { native.attributes.loopBoolean = value }
    actual var scaleType: ImageScaleType = ImageScaleType.Fit
        set(value) {
            field = value
            native.classes.removeAll { it.startsWith("scaleType-") }
            native.classes.add("scaleType-$value")
        }
}
expect val Video.nativeTime: Writable<Double>
expect val Video.nativePlaying: Writable<Boolean>
expect val Video.nativeVolume: Writable<Float>
