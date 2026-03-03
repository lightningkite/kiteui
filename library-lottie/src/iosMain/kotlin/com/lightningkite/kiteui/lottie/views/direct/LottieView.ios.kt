package com.lightningkite.kiteui.lottie.views.direct

import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.lottie.LottieRenderer
import com.lightningkite.kiteui.lottie.LottieColor
import com.lightningkite.kiteui.lottie.applyColorTransform
import com.lightningkite.kiteui.lottie.models.LottieAnimation
import com.lightningkite.kiteui.lottie.models.LottieRaw
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.canvas.DrawingContext2D
import com.lightningkite.kiteui.views.canvas.DrawingContext2DImpl
import com.lightningkite.kiteui.views.canvas.clear
import com.lightningkite.kiteui.views.canvas.height
import com.lightningkite.kiteui.views.canvas.width
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.*
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * iOS implementation of LottieView using pure Kotlin Lottie renderer.
 * Renders animations to a Canvas using the custom LottieRenderer.
 */
@OptIn(ExperimentalForeignApi::class)
actual class LottieView actual constructor(
    context: RContext,
    actual val source: LottieSource,
    actual val description: String,
) : RView(context) {

    private val canvasView = LottieCanvasView()
    override val native: UIView get() = canvasView

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var originalJson: String? = null
    private var animation: LottieAnimation? = null
    private var renderer: LottieRenderer? = null
    private var animationStartTime: TimeSource.Monotonic.ValueTimeMark? = null
    private var pausedProgress: Float = 0f
    private var _colorTransform: ((LottieColor) -> Color)? = null

    private val _state = RawReactive<Unit>()
    actual val state: Reactive<Unit> = _state

    private val _duration = RawReactive<Duration?>(ReactiveState(null))
    actual val duration: Reactive<Duration?> = _duration

    private val _playing = Signal(false)
    actual val playing: MutableReactive<Boolean> = object : MutableReactive<Boolean> {
        override val state: ReactiveState<Boolean> get() = _playing.state
        override suspend fun set(value: Boolean) {
            if (value) play() else pause()
        }
        override fun addListener(listener: () -> Unit): () -> Unit = _playing.addListener(listener)
    }

    private var _loop = true
    actual var loop: Boolean
        get() = _loop
        set(value) { _loop = value }

    private var _speed = 1f
    actual var speed: Float
        get() = _speed
        set(value) { _speed = value }

    private val _progress = Signal(0f)
    actual val progress: MutableReactive<Float> = object : MutableReactive<Float> {
        override val state: ReactiveState<Float> get() = _progress.state
        override suspend fun set(value: Float) { seekToProgress(value) }
        override fun addListener(listener: () -> Unit): () -> Unit = _progress.addListener(listener)
    }

    private var _autoPlay = true
    actual var autoPlay: Boolean
        get() = _autoPlay
        set(value) { _autoPlay = value }

    private val _completedPlay = mutableListOf<() -> Unit>()
    actual val completedPlay: Listenable = object : Listenable {
        override fun addListener(listener: () -> Unit): () -> Unit {
            _completedPlay.add(listener)
            return { _completedPlay.remove(listener) }
        }
    }

    actual var colorTransform: ((LottieColor) -> Color)?
        get() = _colorTransform
        set(value) {
            _colorTransform = value
            reloadWithTransform()
        }

    private fun reloadWithTransform() {
        val json = originalJson ?: return
        val transform = _colorTransform
        val finalJson = if (transform != null) applyColorTransform(json, transform) else json

        val wasPlaying = _playing.state.get()
        val savedProgress = if (wasPlaying) calculateCurrentProgress(animation ?: return) else pausedProgress

        animation = LottieAnimation.parse(finalJson)
        renderer = animation?.let { LottieRenderer(it) }

        pausedProgress = savedProgress
        if (wasPlaying) {
            animationStartTime = TimeSource.Monotonic.markNow()
        }
        canvasView.setNeedsDisplay()
    }

    init {
        canvasView.lottieView = this
        canvasView.accessibilityLabel = description

        // Load animation
        loadAnimation()
    }

    private fun loadAnimation() {
        scope.launch {
            try {
                val json = when (val src = source) {
                    is LottieRaw -> src.json
                    is LottieRemote -> {
                        fetch(src.url).text()
                    }
                }

                originalJson = json
                val finalJson = _colorTransform?.let { applyColorTransform(json, it) } ?: json

                animation = LottieAnimation.parse(finalJson)
                renderer = animation?.let { LottieRenderer(it) }

                animation?.let { anim ->
                    _duration.state = ReactiveState(anim.durationSeconds.seconds)
                }

                _state.state = ReactiveState(Unit)

                if (_autoPlay) {
                    play()
                } else {
                    canvasView.setNeedsDisplay()
                }
            } catch (e: Exception) {
                _state.state = ReactiveState.exception(e)
            }
        }
    }

    actual fun play() {
        if (animation == null) return
        animationStartTime = TimeSource.Monotonic.markNow()
        _playing.value = true
        startAnimationLoop()
    }

    actual fun pause() {
        val anim = animation ?: return
        pausedProgress = calculateCurrentProgress(anim)
        _playing.value = false
        stopAnimationLoop()
    }

    actual fun stop() {
        pausedProgress = 0f
        _progress.value = 0f
        _playing.value = false
        stopAnimationLoop()
        canvasView.setNeedsDisplay()
    }

    actual fun seekToFrame(frame: Int) {
        val anim = animation ?: return
        pausedProgress = (frame.toFloat() / anim.totalFrames.toFloat()).coerceIn(0f, 1f)
        _progress.value = pausedProgress
        if (!_playing.state.get()) {
            canvasView.setNeedsDisplay()
        }
    }

    actual fun seekToProgress(progress: Float) {
        pausedProgress = progress.coerceIn(0f, 1f)
        _progress.value = pausedProgress
        if (!_playing.state.get()) {
            canvasView.setNeedsDisplay()
        }
    }

    private var displayLinkRemover: (() -> Unit)? = null

    private fun startAnimationLoop() {
        if (displayLinkRemover != null) return
        displayLinkRemover = AppState.animationFrame.addListener {
            updateAnimation()
        }
    }

    private fun stopAnimationLoop() {
        displayLinkRemover?.invoke()
        displayLinkRemover = null
    }

    private fun calculateCurrentProgress(anim: LottieAnimation): Float {
        val startTime = animationStartTime ?: return pausedProgress
        val elapsed = startTime.elapsedNow().inWholeMilliseconds / 1000.0
        val progressFromStart = (elapsed * _speed / anim.durationSeconds).toFloat()
        return (pausedProgress + progressFromStart).let {
            if (_loop) it % 1f else it.coerceAtMost(1f)
        }
    }

    private fun updateAnimation() {
        val anim = animation ?: return
        if (!_playing.state.get()) return

        val currentProgress = calculateCurrentProgress(anim)
        _progress.value = currentProgress

        // Check if animation completed
        if (!_loop && currentProgress >= 1f) {
            _playing.value = false
            stopAnimationLoop()
            _completedPlay.forEach { it() }
        }

        canvasView.setNeedsDisplay()
    }

    internal fun draw(context: DrawingContext2D) {
        val anim = animation ?: return
        val rend = renderer ?: return

        context.clear()

        // Calculate current frame
        val progress = if (_playing.state.get()) {
            calculateCurrentProgress(anim)
        } else {
            pausedProgress
        }
        val frame = anim.inPoint + (progress * anim.totalFrames)

        // Scale to fit the canvas
        val canvasWidth = context.width
        val canvasHeight = context.height
        val scaleX = canvasWidth / anim.width.toDouble()
        val scaleY = canvasHeight / anim.height.toDouble()
        val scale = minOf(scaleX, scaleY)

        context.save()
        context.translate(
            (canvasWidth - anim.width * scale) / 2.0,
            (canvasHeight - anim.height * scale) / 2.0
        )
        context.scale(scale, scale)

        rend.render(context, frame)

        context.restore()
    }

    fun cleanup() {
        scope.cancel()
        stopAnimationLoop()
    }
}

/**
 * Custom UIView for rendering Lottie animations.
 */
@OptIn(ExperimentalForeignApi::class)
private class LottieCanvasView : UIView(CGRectMake(0.0, 0.0, 100.0, 100.0)) {
    var lottieView: LottieView? = null

    init {
        opaque = false
        backgroundColor = UIColor.clearColor
        contentMode = UIViewContentMode.UIViewContentModeRedraw
    }

    override fun drawRect(rect: CValue<CGRect>) {
        val ctx = UIGraphicsGetCurrentContext() ?: return
        val width = rect.useContents { size.width }
        val height = rect.useContents { size.height }

        val drawingContext = DrawingContext2DImpl(ctx, width, height)
        lottieView?.draw(drawingContext)
    }

    override fun intrinsicContentSize(): CValue<CGSize> {
        return CGSizeMake(100.0, 100.0)
    }
}
