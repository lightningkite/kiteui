package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGAffineTransformMake
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import kotlin.time.DurationUnit

actual fun RView.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    if(!animationsEnabled) return
    withoutAnimation {
        opacity = 0.0
    }
    launch {
        delay(16)
        withoutAnimation {
            opacity = 1.0
            transition.enter(native)
        }
        UIView.animateWithDuration(
            duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
            completion = { done?.invoke() },
            animations = {
                val before = isInAnimationBlock
                isInAnimationBlock = true
                try {
                    native.transform = CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                    native.alpha = 1.0
                } finally {
                    isInAnimationBlock = before
                }
            }
        )
    }
}
actual fun RView.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    if(!animationsEnabled) return
    UIView.animateWithDuration(
        duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
        completion = { if (!isShutdown) done?.invoke() },
        animations = {
            val before = isInAnimationBlock
            isInAnimationBlock = true
            try {
                transition.exit(native)
            } finally {
                isInAnimationBlock = before
            }
        }
    )
}