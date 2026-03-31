package com.lightningkite.kiteui.lottie.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.kiteui.lottie.models.LottieSource
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.*
import kotlin.time.Duration

/**
 * A view that plays Lottie animations.
 *
 * Lottie is a library for rendering After Effects animations exported as JSON.
 * This view provides cross-platform support for playing these animations with
 * full playback controls.
 *
 * @param context The rendering context
 * @param source The Lottie animation source (Remote URL or raw JSON)
 * @param description Accessibility description for the animation
 */
@ExperimentalKiteUi
@Untested
expect class LottieView(
    context: ElementContext,
    source: LottieSource,
    description: String,
) : NativeElement {
    /** The source of the Lottie animation */
    val source: LottieSource

    /** Accessibility description */
    val description: String

    /**
     * Reactive state indicating load status.
     * Will contain an exception if loading failed.
     */
    val state: Reactive<Unit>

    /** Whether the animation is currently playing */
    val playing: MutableReactive<Boolean>

    /** Whether the animation should loop when it reaches the end */
    var loop: Boolean

    /** Playback speed multiplier (1.0 = normal speed, 2.0 = double speed, etc.) */
    var speed: Float

    /**
     * Current progress of the animation (0.0 to 1.0).
     * Can be set to seek to a specific position.
     */
    val progress: MutableReactive<Float>

    /** Total duration of the animation, null if not yet loaded */
    val duration: Reactive<Duration?>

    /** Whether animation plays automatically when loaded */
    var autoPlay: Boolean

    /** Event triggered when the animation completes a play cycle (not triggered if looping) */
    val completedPlay: Listenable

    /** Play the animation from the current position */
    fun play()

    /** Pause the animation at the current position */
    fun pause()

    /** Stop the animation and reset to beginning */
    fun stop()

    /** Seek to a specific frame number */
    fun seekToFrame(frame: Int)

    /** Seek to a specific progress value (0.0 to 1.0) */
    fun seekToProgress(progress: Float)
}
