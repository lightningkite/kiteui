package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.autoplay
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement

actual val Video.nativeTime: Writable<Double>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { (this.element as? HTMLVideoElement)?.currentTime ?: 0.0 },
        set = {
            onElement { element ->
                (this.element as HTMLVideoElement).currentTime = it
            }
        }
    )
actual val Video.nativePlaying: Writable<Boolean>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { (this.element as? HTMLVideoElement)?.paused?.not() ?: (native.attributes.autoplay != null) },
        set = {
            native.attributes.autoplay = it
            onElement { e ->
                e as HTMLVideoElement
                if(it) e.play().catch {
                    if(it.message?.contains("AbortError") == true) return@catch
                    if(it.message?.contains("NotAllowedError") == true) return@catch
                    Exception("Failed to play ${this}", it).report()
                } else e.pause()
            }
        }
    )
actual val Video.nativeVolume: Writable<Float>
    get() = native.vprop(
        eventName = "volumechange",
        get = { (this.element as? HTMLVideoElement)?.volume?.toFloat() ?: 1f },
        set = {
            onElement { element ->
                (element as HTMLVideoElement).volume = it.toDouble()
            }
        }
    )