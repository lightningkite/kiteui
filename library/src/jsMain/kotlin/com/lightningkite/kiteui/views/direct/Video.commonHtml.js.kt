package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.autoplay
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.withWrite
import org.w3c.dom.HTMLVideoElement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

actual val RawVideoView.nativeTime: MutableReactive<Duration>
    get() = remember {
        rerunOn(AppState.animationFrame)
        (this@nativeTime.native.element as? HTMLVideoElement)?.currentTime?.seconds ?: Duration.ZERO
    }.withWrite { it ->
        this@nativeTime.native.onElement { element ->
            (element as HTMLVideoElement).currentTime = it.toDouble(DurationUnit.SECONDS)
        }
    }
//    get() = native.vprop(
//        eventName = "timeupdate",
//        get = { (this.element as? HTMLVideoElement)?.currentTime ?: 0.0 },
//        set = {
//            onElement { element ->
//                (this.element as HTMLVideoElement).currentTime = it
//            }
//        }
//    )
actual val RawVideoView.nativePlaying: MutableReactive<Boolean>
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
actual val RawVideoView.nativeVolume: MutableReactive<Float>
    get() = native.vprop(
        eventName = "volumechange",
        get = { (this.element as? HTMLVideoElement)?.volume?.toFloat() ?: 1f },
        set = {
            onElement { element ->
                (element as HTMLVideoElement).volume = it.toDouble()
            }
        }
    )

actual fun RawVideoView.nativeLoad(url: String?) {
    native.onElement {
        val v = it as HTMLVideoElement
        v.addEventListener("error", { _state.state = ReactiveState.exception(Exception("Failed to load video")) })
        v.addEventListener("loadeddata", { _state.state = ReactiveState(Unit) })
        v.addEventListener("canplay", { _state.state = ReactiveState(Unit) })
        v.src = url ?: ""
    }
}
actual val RawVideoView.nativeSeekableTimeRanges: List<ClosedFloatingPointRange<Double>>
    get() {
        return (native.element as? HTMLVideoElement)?.seekable?.let {
            (0 until it.length).map { i -> it.start(i)..it.end(i) }
        } ?: listOf()
    }
actual val RawVideoView.nativeDuration: Reactive<Double?>
    get() = native.vread(
        eventName = "durationchange",
        get = {
            (this.element as? HTMLVideoElement)?.duration?.takeIf {
                it.isFinite() && !it.isNaN()
            }
        }
    )