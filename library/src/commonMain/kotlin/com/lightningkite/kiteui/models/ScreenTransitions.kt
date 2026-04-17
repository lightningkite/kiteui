package com.lightningkite.kiteui.models

/**
 * Describes a visual transition effect for screen swaps and show/hide animations.
 *
 * [entryTransform] is the starting state of an appearing element (animates FROM this to identity).
 * [exitTransform] is the ending state of a disappearing element (animates TO this from identity).
 * Translation values are relative: 1.0 = 100% of container dimension.
 */
data class ScreenTransition(
    val name: String,
    val entryTransform: Transformation = Transformation(),
    val exitTransform: Transformation = Transformation(),
    val fade: Boolean = false,
    val easing: Easing = Easing.EaseInOut,
) {
    companion object {
        val None = ScreenTransition("None")
        val Fade = ScreenTransition("Fade", fade = true)
        val Push = ScreenTransition(
            "Push",
            entryTransform = Transformation(translationX = 1.0),
            exitTransform = Transformation(translationX = -1.0),
        )
        val Pop = ScreenTransition(
            "Pop",
            entryTransform = Transformation(translationX = -1.0),
            exitTransform = Transformation(translationX = 1.0),
        )
        val PullUp = ScreenTransition(
            "PullUp",
            entryTransform = Transformation(translationY = 1.0),
            exitTransform = Transformation(translationY = -1.0),
        )
        val PullDown = ScreenTransition(
            "PullDown",
            entryTransform = Transformation(translationY = -1.0),
            exitTransform = Transformation(translationY = 1.0),
        )
        val GrowFade = ScreenTransition(
            "GrowFade",
            entryTransform = Transformation(scaleX = 0.75, scaleY = 0.75),
            exitTransform = Transformation(scaleX = 1.33, scaleY = 1.33),
            fade = true,
        )
        val ShrinkFade = ScreenTransition(
            "ShrinkFade",
            entryTransform = Transformation(scaleX = 1.33, scaleY = 1.33),
            exitTransform = Transformation(scaleX = 0.75, scaleY = 0.75),
            fade = true,
        )
    }
}

data class ScreenTransitions(
    val forward: ScreenTransition,
    val reverse: ScreenTransition,
    val neutral: ScreenTransition,
) {
    companion object {
        val None = ScreenTransitions(
            forward = ScreenTransition.None,
            reverse = ScreenTransition.None,
            neutral = ScreenTransition.None,
        )
        val HorizontalSlide = ScreenTransitions(
            forward = ScreenTransition.Push,
            reverse = ScreenTransition.Pop,
            neutral = ScreenTransition.Fade,
        )
        val Fade = ScreenTransitions(
            forward = ScreenTransition.Fade,
            reverse = ScreenTransition.Fade,
            neutral = ScreenTransition.Fade,
        )
        val FadeResize = ScreenTransitions(
            forward = ScreenTransition.GrowFade,
            reverse = ScreenTransition.ShrinkFade,
            neutral = ScreenTransition.Fade,
        )
        val VerticalSlide = ScreenTransitions(
            forward = ScreenTransition.PullUp,
            reverse = ScreenTransition.PullDown,
            neutral = ScreenTransition.Fade,
        )
    }
}
