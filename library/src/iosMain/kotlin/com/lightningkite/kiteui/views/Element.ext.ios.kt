package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.assertMainThread
import com.lightningkite.kiteui.debugMode
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionTransitionCrossDissolve
import kotlin.time.DurationUnit


var animationsEnabled: Boolean = true
var isInAnimationBlock: Boolean = false

actual val Element.areAnimationsEnabled: Boolean get() = animationsEnabled

actual inline fun Element.withoutAnimation(action: () -> Unit) {
    native.withoutAnimation(action)
}

inline fun UIView.debugPrint(get: () -> String) {
    if (debugMode && Element.Debugger.debugTarget?.native == this)
        Log.tag("viewDebugTarget").info(get())
}

inline fun UIView.withoutAnimation(action: () -> Unit) {
    assertMainThread()
    val before = animationsEnabled
    try {
        animationsEnabled = false
        CATransaction.begin()
        CATransaction.disableActions()
        try {
            action()
        } finally {
            CATransaction.commit()
        }
    } finally {
        animationsEnabled = before
    }
}

inline fun UIView.animateIfAllowed(crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(/*extensionAnimationDuration ?:*/ 0.5) {
        val before = isInAnimationBlock
        isInAnimationBlock = true
        try {
            action()
        } finally {
            isInAnimationBlock = before
        }
    } else {
        action()
    }
}

inline fun Element.animateIfAllowed(crossinline onComplete: () -> Unit = {}, crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.animateWithDuration(
        duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
        completion = { onComplete() },
        animations = {
            val before = isInAnimationBlock
            isInAnimationBlock = true
            try {
                action()
            } finally {
                isInAnimationBlock = before
            }
        }
    ) else {
        action()
        onComplete()
    }
}

inline fun Element.transitionIfAllowed(crossinline onComplete: () -> Unit = {}, crossinline action: () -> Unit) {
    if (animationsEnabled) UIView.transitionWithView(
        view = native,
        duration = theme.transitionDuration.toDouble(DurationUnit.SECONDS),
        completion = { onComplete() },
        animations = {
            val before = isInAnimationBlock
            isInAnimationBlock = true
            try {
                action()
            } finally {
                isInAnimationBlock = before
            }
        },
        options = UIViewAnimationOptionTransitionCrossDissolve
    ) else {
        action()
        onComplete()
    }
}
