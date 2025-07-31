package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.*

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
    public actual val time: Writable<Double> = nativeTime
    public actual val playing: Writable<Boolean> = nativePlaying
    public actual val volume: Writable<Float> = nativeVolume
    public actual var showControls: Boolean
        get() = native.attributes.controls != null
        set(value) { native.attributes.controls = value }
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
public expect val Video.nativeTime: Writable<Double>
public expect val Video.nativePlaying: Writable<Boolean>
public expect val Video.nativeVolume: Writable<Float>
