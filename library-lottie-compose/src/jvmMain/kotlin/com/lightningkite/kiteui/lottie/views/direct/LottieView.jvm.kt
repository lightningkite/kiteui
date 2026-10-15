package com.lightningkite.kiteui.lottie.views.direct

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.lottie.LottieRenderer
import com.lightningkite.kiteui.lottie.models.LottieAnimation
import com.lightningkite.kiteui.lottie.models.LottieRaw
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.canvas.ComposeDrawingContext2D
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.withWrite
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

// Drawn by the common LottieRenderer, as on iOS, so it supports only what that renderer supports.
@ExperimentalKiteUi
@Untested
actual class LottieView actual constructor(
    context: ElementContext,
    actual val source: LottieSource,
    actual val description: String,
) : NativeElement(context) {
    private var animation: LottieAnimation? by mutableStateOf(null)

    private val loadState = RawReactive<Unit>()
    actual val state: Reactive<Unit> = loadState

    private val loadedDuration = Signal<Duration?>(null)
    actual val duration: Reactive<Duration?> = loadedDuration

    private val isPlaying = Signal(false)
    actual val playing: MutableReactive<Boolean> = isPlaying.withWrite { if (it) play() else pause() }

    private val currentProgress = Signal(0f)
    actual val progress: MutableReactive<Float> = currentProgress.withWrite { seekToProgress(it) }
    private var redraws by mutableIntStateOf(0)

    actual var loop: Boolean = true
    actual var speed: Float = 1f
    actual var autoPlay: Boolean = true

    private val completed = BasicListenable()
    actual val completedPlay: Listenable = completed

    private var stopFrames: (() -> Unit)? = null
    private var lastFrame = TimeSource.Monotonic.markNow()

    init {
        currentProgress.addListener { redraws++ }
        onRemove { stopFrames?.invoke() }
        launch {
            try {
                val json = when (source) {
                    is LottieRaw -> source.json
                    is LottieRemote -> {
                        val response = fetch(source.url)
                        if (!response.ok) throw IllegalStateException("Failed to load Lottie animation ${source.url}: HTTP ${response.status}")
                        response.text()
                    }
                }
                val parsed = withContext(Dispatchers.Default) { LottieAnimation.parse(json) }
                animation = parsed
                loadedDuration.value = parsed.durationSeconds.seconds
                loadState.state = ReactiveState(Unit)
                // A play() before loading counts too, as on Android and the web.
                if (autoPlay || isPlaying.value) play()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadState.state = ReactiveState.exception(e)
            }
        }
    }

    actual fun play() {
        isPlaying.value = true
        if (animation == null || stopFrames != null) return
        if (currentProgress.value >= 1f) currentProgress.value = 0f
        lastFrame = TimeSource.Monotonic.markNow()
        stopFrames = AppState.animationFrame.addListener { advance() }
    }

    actual fun pause() {
        isPlaying.value = false
        stopFrames?.invoke()
        stopFrames = null
    }

    actual fun stop() {
        pause()
        currentProgress.value = 0f
    }

    // Frame numbers as in the file (and Android), so an animation whose in-point isn't 0 starts at its in-point.
    actual fun seekToFrame(frame: Int) {
        val animation = checkNotNull(animation) { "Can't seek $source to a frame before it has loaded; wait for state to be ready" }
        seekToProgress(((frame - animation.inPoint) / animation.totalFrames).toFloat())
    }

    actual fun seekToProgress(progress: Float) {
        currentProgress.value = progress.coerceIn(0f, 1f)
    }

    private fun advance() {
        val animation = animation!!
        val elapsed = lastFrame.elapsedNow().toDouble(DurationUnit.SECONDS)
        lastFrame = TimeSource.Monotonic.markNow()
        val next = currentProgress.value + (elapsed * speed / animation.durationSeconds).toFloat()
        when {
            next in 0f..1f -> currentProgress.value = next
            loop -> currentProgress.value = next - floor(next)
            else -> {
                currentProgress.value = next.coerceIn(0f, 1f)
                pause()
                completed.invokeAll()
            }
        }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val textMeasurer = rememberTextMeasurer()
        val animation = animation
        val renderer = remember(animation) { animation?.let { LottieRenderer(it) } }
        Spacer(
            modifier
                // Sized to the animation once loaded, like Android's LottieAnimationView.
                .then(if (animation != null) Modifier.size(animation.width.dp, animation.height.dp) else Modifier)
                .semantics { contentDescription = description }
                .clipToBounds()
                .drawBehind {
                    redraws // Read, so that progress changes redraw.
                    if (animation == null || renderer == null) return@drawBehind
                    val scale = minOf(size.width / animation.width, size.height / animation.height).toDouble()
                    val ctx = ComposeDrawingContext2D(this, textMeasurer)
                    ctx.translate((size.width - animation.width * scale) / 2, (size.height - animation.height * scale) / 2)
                    ctx.scale(scale, scale)
                    renderer.render(ctx, animation.inPoint + currentProgress.value * animation.totalFrames)
                }
        )
    }
}
