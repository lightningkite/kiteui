package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.HTMLElement
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter

@Suppress("ACTUAL_WITHOUT_EXPECT") actual typealias NVideo = HTMLElement
@ViewDsl
actual inline fun ViewWriter.videoActual(crossinline setup: Video.() -> Unit): Unit = todo("Video")
actual inline var Video.source: VideoSource?
    get() = TODO()
    set(value) { }
actual val Video.time: Writable<Double>
    get() = TODO("Not yet implemented")
actual val Video.playing: Writable<Boolean>
    get() = TODO("Not yet implemented")
actual val Video.volume: Writable<Float> get() = TODO()
actual var Video.showControls: Boolean
    get() = TODO()
    set(value) { TODO() }
actual var Video.loop: Boolean
    get() = TODO()
    set(value) { TODO() }
actual var Video.scaleType: ImageScaleType
    get() = TODO()
    set(value) {
        TODO()
    }