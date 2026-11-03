package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.Transformation
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.*
import platform.UIKit.UIView
import kotlin.time.DurationUnit

internal fun Transformation.toCGAffineTransform(view: UIView): CValue<CGAffineTransform> {
    val w = view.superview?.bounds?.useContents { size.width } ?: view.bounds?.useContents { size.width } ?: 0.0
    val h = view.superview?.bounds?.useContents { size.height } ?: view.bounds?.useContents { size.height } ?: 0.0
    var t = CGAffineTransformMakeTranslation(translationX * w, translationY * h)
    if (scaleX != 1.0 || scaleY != 1.0) {
        t = CGAffineTransformScale(t, scaleX, scaleY)
    }
    if (rotation != 0.0) {
        t = CGAffineTransformRotate(t, rotation * kotlin.math.PI / 180.0)
    }
    return t
}

public actual fun Element.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    if (!animationsEnabled) { done?.invoke(); return }
    withoutAnimation {
        opacity = 0.0
    }
    launch {
        delay(16)
        withoutAnimation {
            native.alpha = if (transition.fade) 0.0 else 1.0
            native.transform = transition.entryTransform.toCGAffineTransform(native)
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

public actual fun Element.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    if (!animationsEnabled) { done?.invoke(); return }
    UIView.animateWithDuration(
        duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
        completion = { if (!underlyingNativeElement.isShutdown) done?.invoke() },
        animations = {
            val before = isInAnimationBlock
            isInAnimationBlock = true
            try {
                native.transform = transition.exitTransform.toCGAffineTransform(native)
                if (transition.fade) native.alpha = 0.0
            } finally {
                isInAnimationBlock = before
            }
        }
    )
}
