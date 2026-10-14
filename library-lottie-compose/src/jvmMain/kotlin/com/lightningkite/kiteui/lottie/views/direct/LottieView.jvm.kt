package com.lightningkite.kiteui.lottie.views.direct

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*
import kotlin.time.Duration

// TODO: render with the common LottieRenderer onto a Compose canvas.
@ExperimentalKiteUi
@Untested
actual class LottieView actual constructor(
    context: ElementContext,
    actual val source: LottieSource,
    actual val description: String,
) : NativeElement(context) {
    init {
        throw NotImplementedError("Compose backend: LottieView is not implemented yet")
    }

    actual val state: Reactive<Unit> = Constant(Unit)
    actual val playing: MutableReactive<Boolean> = Signal(false)
    actual var loop: Boolean = false
    actual var speed: Float = 1f
    actual val progress: MutableReactive<Float> = Signal(0f)
    actual val duration: Reactive<Duration?> = Constant(null)
    actual var autoPlay: Boolean = true
    actual val completedPlay: Listenable = BasicListenable()
    actual fun play() {}
    actual fun pause() {}
    actual fun stop() {}
    actual fun seekToFrame(frame: Int) {}
    actual fun seekToProgress(progress: Float) {}

    @Composable
    override fun Content(modifier: Modifier) {}
}
