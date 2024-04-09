package com.lightningkite.kiteui.models

expect class ScreenTransition {
    companion object {
        val None: ScreenTransition
        val Push: ScreenTransition
        val Pop: ScreenTransition
        val PullDown: ScreenTransition
        val PullUp: ScreenTransition
        val Fade: ScreenTransition
        val GrowFade: ScreenTransition
        val ShrinkFade: ScreenTransition
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
