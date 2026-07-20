package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

public actual class RawVideoView actual constructor(
    context: ElementContext,
    public actual val source: VideoSource,
    public actual val description: String,
    public actual val scaleType: ImageScaleType,
    public actual val preloadHint: PreloadHint,
) : NativeElement(context) {
    public val _state: RawReactive<Unit> = RawReactive<Unit>()
    public actual val state: Reactive<Unit> = _state

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

    public actual val time: MutableReactive<Double> = nativeTime.lens(get = {it.toDouble(DurationUnit.SECONDS)}, set = {it.seconds})
    public actual val currentTime: MutableReactive<Duration> = nativeTime
    public actual val playing: MutableReactive<Boolean> = nativePlaying
    public actual val volume: MutableReactive<Float> = nativeVolume
    public actual val sourceDuration: Reactive<Double?> = nativeDuration

    public actual var showControls: Boolean
        get() = native.attributes.controls != null
        set(value) {
            native.attributes.controls = value
            native.attributes.playsInline = !value
        }

    public actual var loop: Boolean
        get() = native.attributes.loopBoolean != null
        set(value) { native.attributes.loopBoolean = value }

    public actual val completedPlay: Listenable = native.vevent("ended")
    public actual val seekableTimeRanges: List<ClosedFloatingPointRange<Double>> = nativeSeekableTimeRanges
}

public expect val RawVideoView.nativeTime: MutableReactive<Duration>
public expect val RawVideoView.nativePlaying: MutableReactive<Boolean>
public expect val RawVideoView.nativeVolume: MutableReactive<Float>
public expect val RawVideoView.nativeSeekableTimeRanges: List<ClosedFloatingPointRange<Double>>
public expect val RawVideoView.nativeDuration: Reactive<Double?>

public expect fun RawVideoView.nativeLoad(url: String?)
