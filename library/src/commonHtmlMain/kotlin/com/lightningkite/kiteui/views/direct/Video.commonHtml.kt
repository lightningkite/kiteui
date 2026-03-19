package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

actual class RawVideoView actual constructor(
    context: ElementContext,
    actual val source: VideoSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
    actual val preloadHint: PreloadHint,
) : NativeElement(context) {
    val _state = RawReactive<Unit>()
    actual val state: Reactive<Unit> = _state

    init {
        native.tag = "video"
        native.classes.add("viewDraws")
        native.classes.add("scaleType-$scaleType")
        // Set initial attributes
        when (val value = source) {
            is VideoRemote -> native.attributes.src = value.url
            is VideoRaw -> native.attributes.src = createObjectURL(value.data)
            is VideoResource -> native.attributes.src = context.basePath + value.relativeUrl
            is VideoLocal -> native.attributes.src = createObjectURL(value.file)
            else -> native.attributes.src = ""
        }
        nativeLoad(native.attributes.src)
        native.attributes.preload = when(preloadHint) {
            PreloadHint.NONE -> "none"
            PreloadHint.METADATA -> "metadata"
            PreloadHint.ALL -> "auto"
        }
    }

    actual val time: MutableReactive<Double> = nativeTime.lens(get = {it.toDouble(DurationUnit.SECONDS)}, set = {it.seconds})
    actual val currentTime: MutableReactive<Duration> = nativeTime
    actual val playing: MutableReactive<Boolean> = nativePlaying
    actual val volume: MutableReactive<Float> = nativeVolume
    actual val sourceDuration: Reactive<Double?> = nativeDuration

    actual var showControls: Boolean
        get() = native.attributes.controls != null
        set(value) {
            native.attributes.controls = value
            native.attributes.playsInline = !value
        }

    actual var loop: Boolean
        get() = native.attributes.loopBoolean != null
        set(value) { native.attributes.loopBoolean = value }

    actual val completedPlay: Listenable = native.vevent("ended")
    actual val seekableTimeRanges: List<ClosedFloatingPointRange<Double>> = nativeSeekableTimeRanges
}

expect val RawVideoView.nativeTime: MutableReactive<Duration>
expect val RawVideoView.nativePlaying: MutableReactive<Boolean>
expect val RawVideoView.nativeVolume: MutableReactive<Float>
expect val RawVideoView.nativeSeekableTimeRanges: List<ClosedFloatingPointRange<Double>>
expect val RawVideoView.nativeDuration: Reactive<Double?>

expect fun RawVideoView.nativeLoad(url: String?)
