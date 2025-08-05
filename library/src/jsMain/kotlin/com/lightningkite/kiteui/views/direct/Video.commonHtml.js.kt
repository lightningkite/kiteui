package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.autoplay
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLVideoElement

@InternalKiteUi
public actual val Video.nativeTime: MutableReactive<Double>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { (this.element as? HTMLVideoElement)?.currentTime ?: 0.0 },
        set = {
            onElement { element ->
                (this.element as HTMLVideoElement).currentTime = it
            }
        }
    )
@InternalKiteUi
public actual val Video.nativePlaying: MutableReactive<Boolean>
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
@InternalKiteUi
public actual val Video.nativeVolume: MutableReactive<Float>
    get() = native.vprop(
        eventName = "volumechange",
        get = { (this.element as? HTMLVideoElement)?.volume?.toFloat() ?: 1f },
        set = {
            onElement { element ->
                (element as HTMLVideoElement).volume = it.toDouble()
            }
        }
    )