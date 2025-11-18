package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.src
import com.lightningkite.kiteui.views.autoplay
import com.lightningkite.reactive.core.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

actual val RawVideoView.nativeTime: MutableReactive<Duration>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { attributes["data-currentTime"]?.toDoubleOrNull()?.seconds ?: Duration.ZERO },
        set = { value -> setAttribute("data-currentTime", value.toString()) }
    )

actual val RawVideoView.nativePlaying: MutableReactive<Boolean>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { attributes.autoplay ?: (attributes["data-playing"]?.toBoolean() ?: false) },
        set = { value ->
            attributes.autoplay = value
            setAttribute("data-playing", value.toString())
        }
    )

actual val RawVideoView.nativeVolume: MutableReactiveValue<Float>
    get() = native.vprop(
        eventName = "volumechange",
        get = { attributes["data-volume"]?.toFloatOrNull() ?: 1f },
        set = { value ->
            setAttribute("data-volume", value.toString())
            if(value == 0f) setAttribute("muted", "true")
            else setAttribute("muted", null)
        }
    )

actual fun RawVideoView.nativeLoad(url: String?) {
    native.attributes.src = url
}
actual val RawVideoView.nativeSeekableTimeRanges: List<ClosedFloatingPointRange<Double>> get() = listOf()
actual val RawVideoView.nativeDuration: Reactive<Double?> get() = Reactive.Never