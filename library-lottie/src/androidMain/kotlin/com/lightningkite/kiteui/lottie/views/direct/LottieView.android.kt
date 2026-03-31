package com.lightningkite.kiteui.lottie.views.direct

import android.animation.Animator
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.lightningkite.kiteui.lottie.models.LottieRaw
import com.lightningkite.kiteui.lottie.models.LottieRemote
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual class LottieView actual constructor(
    context: ElementContext,
    actual val source: LottieSource,
    actual val description: String,
) : NativeElement(context) {

    private val lottieView = LottieAnimationView(context.activity).apply {
        contentDescription = description
    }
    override val native: android.view.View get() = lottieView

    private val _state = RawReactive<Unit>()
    actual val state: Reactive<Unit> = _state

    private val _duration = RawReactive<Duration?>(ReactiveState(null))
    actual val duration: Reactive<Duration?> = _duration

    init {
        when (val value = source) {
            is LottieRemote -> {
                LottieCompositionFactory.fromUrl(context.activity, value.url)
                    .addListener { composition ->
                        lottieView.setComposition(composition)
                        _duration.state = ReactiveState(composition.duration.toLong().milliseconds)
                        _state.state = ReactiveState(Unit)
                        if (autoPlay) lottieView.playAnimation()
                    }
                    .addFailureListener { e ->
                        _state.state = ReactiveState.exception(Exception(e))
                    }
            }
            is LottieRaw -> {
                val cacheKey = value.cacheKey ?: value.json.hashCode().toString()
                LottieCompositionFactory.fromJsonString(value.json, cacheKey)
                    .addListener { composition ->
                        lottieView.setComposition(composition)
                        _duration.state = ReactiveState(composition.duration.toLong().milliseconds)
                        _state.state = ReactiveState(Unit)
                        if (autoPlay) lottieView.playAnimation()
                    }
                    .addFailureListener { e ->
                        _state.state = ReactiveState.exception(Exception(e))
                    }
            }
        }

        onRemove {
            lottieView.cancelAnimation()
        }
    }

    actual val playing: MutableReactive<Boolean> = object : MutableReactive<Boolean> {
        override suspend fun set(value: Boolean) {
            if (value) {
                lottieView.resumeAnimation()
            } else {
                lottieView.pauseAnimation()
            }
        }

        override val state: ReactiveState<Boolean> get() = ReactiveState(lottieView.isAnimating)

        override fun addListener(listener: () -> Unit): () -> Unit {
            val animatorListener = object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) = listener()
                override fun onAnimationEnd(animation: Animator) = listener()
                override fun onAnimationCancel(animation: Animator) = listener()
                override fun onAnimationRepeat(animation: Animator) {}
            }
            lottieView.addAnimatorListener(animatorListener)
            return { lottieView.removeAnimatorListener(animatorListener) }
        }
    }

    actual var loop: Boolean
        get() = lottieView.repeatCount == LottieDrawable.INFINITE
        set(value) {
            lottieView.repeatCount = if (value) LottieDrawable.INFINITE else 0
        }

    actual var speed: Float
        get() = lottieView.speed
        set(value) {
            lottieView.speed = value
        }

    actual val progress: MutableReactive<Float> = object : MutableReactive<Float> {
        override suspend fun set(value: Float) {
            lottieView.progress = value
        }

        override val state: ReactiveState<Float> get() = ReactiveState(lottieView.progress)

        override fun addListener(listener: () -> Unit): () -> Unit {
            var remover: (() -> Unit)? = null
            val animatorListener = object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    remover = AppState.animationFrame.addListener(listener)
                }
                override fun onAnimationEnd(animation: Animator) {
                    remover?.invoke()
                    remover = null
                }
                override fun onAnimationCancel(animation: Animator) {
                    remover?.invoke()
                    remover = null
                }
                override fun onAnimationRepeat(animation: Animator) {}
            }
            lottieView.addAnimatorListener(animatorListener)
            return {
                remover?.invoke()
                lottieView.removeAnimatorListener(animatorListener)
            }
        }
    }

    actual var autoPlay: Boolean = true

    actual val completedPlay: Listenable = object : Listenable {
        override fun addListener(listener: () -> Unit): () -> Unit {
            val animatorListener = object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    if (lottieView.repeatCount != LottieDrawable.INFINITE) {
                        listener()
                    }
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            }
            lottieView.addAnimatorListener(animatorListener)
            return { lottieView.removeAnimatorListener(animatorListener) }
        }
    }

    actual fun play() {
        lottieView.playAnimation()
    }

    actual fun pause() {
        lottieView.pauseAnimation()
    }

    actual fun stop() {
        lottieView.cancelAnimation()
        lottieView.progress = 0f
    }

    actual fun seekToFrame(frame: Int) {
        lottieView.frame = frame
    }

    actual fun seekToProgress(progress: Float) {
        lottieView.progress = progress
    }
}
