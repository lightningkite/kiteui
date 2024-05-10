package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.DynamicCss.basePath
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl

actual class Video actual constructor(context: RContext) : RView(context) {
    actual inline var source: VideoSource?
        get() = TODO()
        set(value) {
            when(value) {
                null -> native.attributes["src"] = ""
                is VideoRemote -> native.attributes["src"] = value.url
                is VideoRaw -> native.attributes["src"] = createObjectURL(value.data)
                is VideoResource -> native.attributes["src"] = basePath + value.relativeUrl
                is VideoLocal -> native.attributes["src"] = createObjectURL(value.file)
                else -> {}
            }
        }
    actual val time: Writable<Double> = nativeTime
    actual val playing: Writable<Boolean> = nativePlaying
    actual val volume: Writable<Float> = nativeVolume
    actual var showControls: Boolean
        get() = native.attributes["controls"] != null
        set(value) { native.attributes["controls"] = value.takeIf { it }?.toString() }
    actual var loop: Boolean
        get() = native.attributes["loop"] != null
        set(value) { native.attributes["loop"] = value.takeIf { it }?.toString() }
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
//actual val Video.nativeTime: Writable<Double> get() = native.vprop("timeupdate", { this.currentTime }, { this.currentTime = it })
//actual val Video.nativePlaying: Writable<Boolean> get() = native.vprop("timeupdate", { !this.paused }, {
//    autoplay = it
//    if (it) play() else pause()
//})
//actual val Video.nativeVolume: Writable<Float> get() = native.vprop("volumechange", { this.volume.toFloat() }, {
//    this.volume = it.toDouble()
//})