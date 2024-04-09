package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.HTMLElement
import com.lightningkite.kiteui.dom.HTMLVideoElement
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import org.w3c.dom.url.URL
import org.w3c.files.Blob

@Suppress("ACTUAL_WITHOUT_EXPECT") 
actual typealias NVideo = org.w3c.dom.HTMLVideoElement

@ViewDsl
actual inline fun ViewWriter.videoActual(crossinline setup: Video.() -> Unit): Unit =
    themedElement<NVideo>("video") {
        controls = true
        setup(Video(this))
    }
actual inline var Video.source: VideoSource?
    get() = TODO()
    set(value) {
        when(value) {
            null -> native.src = ""
            is VideoRemote -> native.src = value.url
            is VideoRaw -> native.src = URL.createObjectURL(Blob(arrayOf(value.data)))
            is VideoResource -> native.src = basePath + value.relativeUrl
            is VideoLocal -> native.src = URL.createObjectURL(value.file)
            else -> {}
        }
    }
actual val Video.time: Writable<Double> get() = native.vprop("timeupdate", { this.currentTime }, { this.currentTime = it })
actual val Video.playing: Writable<Boolean> get() = native.vprop("timeupdate", { !this.paused }, {
    if (it) play().catch { _ ->
        autoplay = it
    } else pause()
})
actual val Video.volume: Writable<Float> get() = native.vprop("volumechange", { this.volume.toFloat() }, {
    this.volume = it.toDouble()
})
actual var Video.showControls: Boolean
    get() = native.controls
    set(value) { native.controls = value }
actual var Video.loop: Boolean
    get() = native.loop
    set(value) { native.loop = value }
actual var Video.scaleType: ImageScaleType
    get() = TODO()
    set(value) {
        native.className = native.className.split(' ').filter { !it.startsWith("scaleType-") }.plus("scaleType-$value").joinToString(" ")
    }