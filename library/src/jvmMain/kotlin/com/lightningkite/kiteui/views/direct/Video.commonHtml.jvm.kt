package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.src
import com.lightningkite.kiteui.views.autoplay
import com.lightningkite.reactive.core.MutableReactive

actual val RawVideoView.nativeTime: MutableReactive<Double>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { attributes["data-currentTime"]?.toDoubleOrNull() ?: 0.0 },
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

actual val RawVideoView.nativeVolume: MutableReactive<Float>
    get() = native.vprop(
        eventName = "volumechange",
        get = { attributes["data-volume"]?.toFloatOrNull() ?: 1f },
        set = { value -> setAttribute("data-volume", value.toString()) }
    )

actual fun RawVideoView.nativeLoad(url: String?) {
    native.attributes.src = url
}
actual val RawVideoView.nativeSeekableTimeRanges: List<ClosedFloatingPointRange<Double>> get() = listOf()