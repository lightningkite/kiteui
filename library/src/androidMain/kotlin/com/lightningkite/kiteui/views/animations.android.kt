package com.lightningkite.kiteui.views

import android.view.animation.PathInterpolator
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.models.Transformation

private fun Transformation.applyToView(view: android.view.View) {
    val parent = view.parent as? android.view.View
    val w = parent?.width?.toFloat() ?: view.width.toFloat()
    val h = parent?.height?.toFloat() ?: view.height.toFloat()
    view.translationX = (translationX * w).toFloat()
    view.translationY = (translationY * h).toFloat()
    view.scaleX = scaleX.toFloat()
    view.scaleY = scaleY.toFloat()
    view.rotation = rotation.toFloat()
    view.rotationX = rotationX.toFloat()
    view.rotationY = rotationY.toFloat()
}

private fun ScreenTransition.toInterpolator() =
    PathInterpolator(easing.x1, easing.y1, easing.x2, easing.y2)

public actual fun Element.animateIn(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    if (!animationsEnabled) { done?.invoke(); return }
    val view = native
    // Set starting state
    transition.entryTransform.applyToView(view)
    if (transition.fade) view.alpha = 0f

    view.animate()
        .translationX(0f)
        .translationY(0f)
        .scaleX(1f)
        .scaleY(1f)
        .rotation(0f)
        .rotationX(0f)
        .rotationY(0f)
        .alpha(1f)
        .setDuration(theme.transitionDuration.inWholeMilliseconds)
        .setInterpolator(transition.toInterpolator())
        .withEndAction { done?.invoke() }
        .start()
}

public actual fun Element.animateOut(
    transition: ScreenTransition,
    done: (() -> Unit)?
) {
    if (!animationsEnabled) { done?.invoke(); return }
    val view = native
    val animator = view.animate()
        .setDuration(theme.transitionDuration.inWholeMilliseconds)
        .setInterpolator(transition.toInterpolator())
        .withEndAction { done?.invoke() }

    val t = transition.exitTransform
    val parent = view.parent as? android.view.View
    val w = parent?.width?.toFloat() ?: view.width.toFloat()
    val h = parent?.height?.toFloat() ?: view.height.toFloat()
    animator.translationX((t.translationX * w).toFloat())
    animator.translationY((t.translationY * h).toFloat())
    animator.scaleX(t.scaleX.toFloat())
    animator.scaleY(t.scaleY.toFloat())
    animator.rotation(t.rotation.toFloat())
    animator.rotationX(t.rotationX.toFloat())
    animator.rotationY(t.rotationY.toFloat())
    if (transition.fade) animator.alpha(0f)

    animator.start()
}
