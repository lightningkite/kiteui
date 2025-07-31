package com.lightningkite.kiteui.models

import android.view.Gravity
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.transition.*
import androidx.transition.Visibility.MODE_IN
import androidx.transition.Visibility.MODE_OUT


public actual class ScreenTransition(
    val name: String,
    val enter: () -> Transition?,
    val exit: () -> Transition?,
) {
    public actual companion object {
        public actual val None: ScreenTransition
            get() = ScreenTransition(
                "None",
                enter = { null },
                exit = { null }
            )
        public actual val Push: ScreenTransition
            get() = ScreenTransition(
                "Push",
                enter = { Slide(Gravity.RIGHT).setInterpolator(DecelerateInterpolator()) },
                exit = { Slide(Gravity.LEFT).setInterpolator(DecelerateInterpolator()) },
            )
        public actual val Pop: ScreenTransition
            get() = ScreenTransition(
                name = "Pop",
                enter = { Slide(Gravity.LEFT).setInterpolator(DecelerateInterpolator()) },
                exit = { Slide(Gravity.RIGHT).setInterpolator(DecelerateInterpolator()) },
            )
        public actual val PullDown: ScreenTransition
            get() = ScreenTransition(
                name = "Pull Down",
                enter = { null },
                exit = { Slide(Gravity.BOTTOM).setInterpolator(DecelerateInterpolator()) }
            )
        public actual val PullUp: ScreenTransition
            get() = ScreenTransition(
                name = "Pull up",
                enter = { Slide(Gravity.BOTTOM).setInterpolator(DecelerateInterpolator()) },
                exit = { null }
            )
        public actual val Fade: ScreenTransition
            get() = ScreenTransition(
                name = "Fade",
                enter = { Fade(MODE_IN) },
                exit = { Fade(MODE_OUT) }
            )

        public actual val GrowFade: ScreenTransition
            get() = ScreenTransition(
                "Grow Fade",
                enter = { Fade(MODE_IN) },
                exit = { Fade(MODE_OUT) }
            )
        public actual val ShrinkFade: ScreenTransition
            get() = ScreenTransition(
                name = "Shrink Fade",
                enter = { Fade(MODE_IN) },
                exit = { Fade(MODE_OUT) }
            )
    }
}
