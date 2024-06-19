package com.lightningkite.kiteui.views.direct

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import com.lightningkite.kiteui.views.ViewWriter
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.animation.addListener
import androidx.transition.*
import com.lightningkite.kiteui.PerformanceInfo
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.views.*


actual class SwapView actual constructor(context: RContext) : RView(context) {
    override val native = FrameLayout(context.activity).apply {
        isClickable = true
    }

    companion object {
        val swapTimeMakeViewPerformance = PerformanceInfo("swapTimeMakeView")
        val swapTimeAddViewsPerformance = PerformanceInfo("swapTimeAddViews")
    }

    actual fun swap(
        transition: ScreenTransition,
        createNewView: ViewWriter.() -> Unit,
    ) {
        val oldView = this.children.firstOrNull()
        var newViewHolder: RView? = null
        val writer = object : ViewWriter() {
            override val context: RContext
                get() = this@SwapView.context

            override fun addChild(view: RView) {
                newViewHolder = view
            }
        }
        animationsEnabled = false
        try {
            swapTimeMakeViewPerformance {
                writer.createNewView()
            }
        } finally {
            animationsEnabled = true
        }
        val newView = newViewHolder
        swapTimeAddViewsPerformance {
            newView?.native?.layoutParams = newView?.native?.layoutParams?.also {
                it.width = ViewGroup.LayoutParams.MATCH_PARENT
                it.height = ViewGroup.LayoutParams.MATCH_PARENT
            } ?: FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            TransitionManager.beginDelayedTransition(native, TransitionSet().apply {
                val enter = transition.enter()
                val exit = transition.exit()
                newView?.let {
                    enter?.setDuration(theme.transitionDuration.inWholeMilliseconds)?.addTarget(it.native)
                }
                oldView?.let {
                    exit?.setDuration(theme.transitionDuration.inWholeMilliseconds)?.addTarget(it.native)
                }
                if(oldView != null && newView != null) {
                    val new = newView.walkTopDown().mapNotNull { it.transitionId?.let { id -> id to it } }.associate { it }
                    val old = oldView.walkTopDown().mapNotNull { it.transitionId?.let { id -> id to it } }.associate { it }
                    val intersecting = new.keys.intersect(old.keys)
                    if(intersecting.isNotEmpty()) {
                        println("Transitioning ${intersecting}")
                        val shared = CustomTransition(native)
                        shared.setDuration(theme.transitionDuration.inWholeMilliseconds)
                        intersecting.forEach {
                            val o = old[it]!!.native
                            val n = new[it]!!.native
//                        exit?.excludeTarget(o, true)
//                        exit?.excludeTarget(n, true)
//                        enter?.excludeTarget(o, true)
//                        enter?.excludeTarget(n, true)
                            shared.addTarget(o)
                            shared.addTarget(n)
                        }
                        addTransition(shared)
                    }
                }
                exit?.let { addTransition(it) }
                enter?.let { addTransition(it) }
            })
            oldView?.let { oldNN -> removeChild(oldNN) }
            newView?.let { addChild(it) }
            if (newView == null) native.visibility = View.GONE
            else native.visibility = View.VISIBLE
        }
    }
}

private fun RView.walkTopDown(): Sequence<RView> = sequenceOf(this) + children.asSequence().flatMap { it.walkTopDown() }

private class CustomTransition(val fromRoot: ViewGroup): Transition() {
    override fun captureStartValues(transitionValues: TransitionValues) {
        val r = android.graphics.Rect()
        transitionValues.view.getDrawingRect(r)
        fromRoot.offsetDescendantRectToMyCoords(transitionValues.view, r)
        transitionValues.values["relativeToRoot"] = r
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        val r = android.graphics.Rect()
        transitionValues.view.getDrawingRect(r)
        fromRoot.offsetDescendantRectToMyCoords(transitionValues.view, r)
        transitionValues.values["relativeToRoot"] = r
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        val startDummy = startValues?.view?.toBitmapDrawable() ?: return null
        val endDummy = endValues?.view?.toBitmapDrawable() ?: return null
        val startRect = (startValues?.values?.get("relativeToRoot") as? android.graphics.Rect) ?: return null
        val endRect = (endValues?.values?.get("relativeToRoot") as? android.graphics.Rect) ?: return null
        startDummy.setBounds(startRect)
        endDummy.setBounds(startRect)
        sceneRoot.overlay.add(startDummy)
        sceneRoot.overlay.add(endDummy)
        startValues.view.visibility = View.INVISIBLE
        endValues.view.visibility = View.INVISIBLE
        return TypedValueAnimator.FloatAnimator(0f, 1f).apply {
            val midRect = android.graphics.Rect()
            onUpdate {
                midRect.set(
                    (startRect.left * (1f - it) + endRect.left * it).toInt(),
                    (startRect.top * (1f - it) + endRect.top * it).toInt(),
                    (startRect.right * (1f - it) + endRect.right * it).toInt(),
                    (startRect.bottom * (1f - it) + endRect.bottom * it).toInt(),
                )
                startDummy.setBounds(midRect)
                startDummy.alpha = ((1f - it) * 0xFF).toInt()
                endDummy.alpha = (it * 0xFF).toInt()
                endDummy.setBounds(midRect)
            }
            addListener(onEnd = {
                sceneRoot.overlay.remove(startDummy)
                sceneRoot.overlay.remove(endDummy)
                endValues.view.visibility = View.VISIBLE
            })
        }
    }
}

private class OverlayChangeBounds : ChangeBounds() {

    override fun createAnimator(sceneRoot: ViewGroup, startValues: TransitionValues?, endValues: TransitionValues?): Animator? {
        if (startValues == null || endValues == null) return super.createAnimator(sceneRoot, startValues, endValues)

        val startView = startValues.view
        val endView = endValues.view

        if (endView.id in targetIds || targetNames?.contains(endView.transitionName) == true) {
            startView.visibility = View.INVISIBLE
            endView.visibility = View.INVISIBLE

            endValues.view = startValues.view.toImageView()
            sceneRoot.overlay.add(endValues.view)

            return super.createAnimator(sceneRoot, startValues, endValues)?.apply {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        endView.visibility = View.VISIBLE
                        sceneRoot.overlay.remove(endValues.view)
                    }
                })
            }
        }

        return super.createAnimator(sceneRoot, startValues, endValues)
    }
}

private fun View.toImageView(): android.widget.ImageView {
    val v = this
    val drawable = toBitmapDrawable()
    return android.widget.ImageView(context).apply {
        setImageDrawable(drawable)
        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        layout(v.left, v.top, v.right, v.bottom)
    }
}

private fun View.toBitmapDrawable(): BitmapDrawable {
    println("Bitmapifying $this, ${(this as? ImageView)?.drawable}")
    val b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    draw(android.graphics.Canvas(b))
    return BitmapDrawable(resources, b)
}
