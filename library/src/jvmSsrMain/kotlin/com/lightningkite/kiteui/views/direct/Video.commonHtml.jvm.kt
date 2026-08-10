package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.src
import com.lightningkite.kiteui.views.autoplay
import com.lightningkite.reactive.core.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public actual val RawVideoView.nativeTime: MutableReactive<Duration>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { attributes["data-currentTime"]?.toDoubleOrNull()?.seconds ?: Duration.ZERO },
        set = { value -> setAttribute("data-currentTime", value.toString()) }
    )

public actual val RawVideoView.nativePlaying: MutableReactive<Boolean>
    get() = native.vprop(
        eventName = "timeupdate",
        get = { attributes.autoplay ?: (attributes["data-playing"]?.toBoolean() ?: false) },
        set = { value ->
            attributes.autoplay = value
            setAttribute("data-playing", value.toString())
        }
    )

public actual val RawVideoView.nativeVolume: MutableReactive<Float>
    get() = native.vprop(
        eventName = "volumechange",
        get = { attributes["data-volume"]?.toFloatOrNull() ?: 1f },
        set = { value -> setAttribute("data-volume", value.toString()) }
    )

public actual fun RawVideoView.nativeLoad(url: String?) {
    native.attributes.src = url
}
public actual val RawVideoView.nativeSeekableTimeRanges: List<ClosedFloatingPointRange<Double>> get() = listOf()
public actual val RawVideoView.nativeDuration: Reactive<Double?> get() = Reactive.Never