package com.lightningkite.kiteui.models

public expect class ScreenTransition {
    public companion object {
        public val None: ScreenTransition
        public val Push: ScreenTransition
        public val Pop: ScreenTransition
        public val PullDown: ScreenTransition
        public val PullUp: ScreenTransition
        public val Fade: ScreenTransition
        public val GrowFade: ScreenTransition
        public val ShrinkFade: ScreenTransition
    }
}

public data class ScreenTransitions(
    public val forward: ScreenTransition,
    public val reverse: ScreenTransition,
    public val neutral: ScreenTransition,
) {
    public companion object {
        public val None: ScreenTransitions = ScreenTransitions(
            forward = ScreenTransition.None,
            reverse = ScreenTransition.None,
            neutral = ScreenTransition.None,
        )
        public val HorizontalSlide: ScreenTransitions = ScreenTransitions(
            forward = ScreenTransition.Push,
            reverse = ScreenTransition.Pop,
            neutral = ScreenTransition.Fade,
        )
        public val Fade: ScreenTransitions = ScreenTransitions(
            forward = ScreenTransition.Fade,
            reverse = ScreenTransition.Fade,
            neutral = ScreenTransition.Fade,
        )
        public val FadeResize: ScreenTransitions = ScreenTransitions(
            forward = ScreenTransition.GrowFade,
            reverse = ScreenTransition.ShrinkFade,
            neutral = ScreenTransition.Fade,
        )
        public val VerticalSlide: ScreenTransitions = ScreenTransitions(
            forward = ScreenTransition.PullUp,
            reverse = ScreenTransition.PullDown,
            neutral = ScreenTransition.Fade,
        )
    }
}
