package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.coordinatorlayout.widget.CoordinatorLayout
import kotlin.math.absoluteValue
import kotlin.math.atan

public class CoordinatorLayoutWithGestures(context: Context) : CoordinatorLayout(context) {

    public var onLeftSwipeAction: (() -> Unit)? = null
    public var onRightSwipeAction: (() -> Unit)? = null

    private val gesturesEnabled: Boolean
        get() = onLeftSwipeAction != null || onRightSwipeAction != null

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val angle = atan((velocityY / velocityX).toDouble())
                if (angle.absoluteValue < Math.PI / 10) {
                    val action = if (velocityX > 0) onRightSwipeAction else onLeftSwipeAction
                    action?.let {
                        it.invoke()
                        return true
                    }
                }
                return false
            }
        }
    )

    public override fun onTouchEvent(ev: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(ev).takeIf { gesturesEnabled } ?: false
    }

    public override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(ev).takeIf { gesturesEnabled } ?: false
    }
}