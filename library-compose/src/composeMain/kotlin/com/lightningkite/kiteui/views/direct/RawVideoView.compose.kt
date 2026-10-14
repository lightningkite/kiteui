package com.lightningkite.kiteui.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlinx.datetime.*
import kotlin.time.Duration

public actual class RawVideoView actual constructor(context: ElementContext, source: VideoSource, description: String, scaleType: ImageScaleType, preloadHint: PreloadHint) : NativeElement(context) {
    init { throw NotImplementedError("Compose backend: RawVideoView is not implemented yet") }

    public actual val source: VideoSource = source
    public actual val description: String = description
    public actual val scaleType: ImageScaleType = scaleType
    public actual val preloadHint: PreloadHint = preloadHint
    public actual val state: Reactive<Unit> = Constant(Unit)
    public actual val seekableTimeRanges: List<ClosedFloatingPointRange<Double>> = emptyList()
    @Deprecated("Use currentTime instead")
    public actual val time: MutableReactive<Double> = Signal(0.0)
    public actual val currentTime: MutableReactive<Duration> = Signal(Duration.ZERO)
    public actual val sourceDuration: Reactive<Double?> = Constant(null)
    public actual val playing: MutableReactive<Boolean> = Signal(false)
    public actual val volume: MutableReactive<Float> = Signal(1f)
    public actual var showControls: Boolean = false
    public actual var loop: Boolean = false
    public actual val completedPlay: Listenable = BasicListenable()

    @Composable
    override fun Content(modifier: Modifier) {}
}
