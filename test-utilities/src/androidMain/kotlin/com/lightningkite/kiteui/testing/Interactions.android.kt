package com.lightningkite.kiteui.testing

import android.text.InputType
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.views.RView

/**
 * Android implementation of Interactions.
 */
actual object Interactions {
    actual fun click(view: RView) {
        // Call performClick on the native Android View
        view.native.performClick()
    }

    actual fun typeText(view: RView, text: String) {
        val nativeView = view.native
        if (nativeView is EditText) {
            // Append text to existing content (simulating typing)
            val currentText = nativeView.text.toString()
            nativeView.setText(currentText + text)
            // Move cursor to end
            nativeView.setSelection(nativeView.text.length)
        } else {
            throw IllegalArgumentException("View is not a text input: ${nativeView::class.simpleName}")
        }
    }

    actual fun clearText(view: RView) {
        val nativeView = view.native
        if (nativeView is EditText) {
            nativeView.setText("")
        } else {
            throw IllegalArgumentException("View is not a text input: ${nativeView::class.simpleName}")
        }
    }

    actual fun setText(view: RView, text: String) {
        val nativeView = view.native
        if (nativeView is EditText) {
            nativeView.setText(text)
            // Move cursor to end
            nativeView.setSelection(text.length)
        } else {
            throw IllegalArgumentException("View is not a text input: ${nativeView::class.simpleName}")
        }
    }

    actual fun scroll(view: RView, deltaX: Int, deltaY: Int) {
        val nativeView = view.native
        when (nativeView) {
            is ScrollView -> {
                nativeView.scrollBy(deltaX, deltaY)
            }
            is NestedScrollView -> {
                nativeView.scrollBy(deltaX, deltaY)
            }
            else -> {
                // Try generic scrollBy
                try {
                    nativeView.scrollBy(deltaX, deltaY)
                } catch (e: Exception) {
                    throw IllegalArgumentException("View is not scrollable: ${nativeView::class.simpleName}")
                }
            }
        }
    }

    actual fun longClick(view: RView) {
        view.native.performLongClick()
    }

    actual fun swipe(view: RView, startX: Float, startY: Float, endX: Float, endY: Float) {
        val nativeView = view.native
        val width = nativeView.width.toFloat()
        val height = nativeView.height.toFloat()

        val startXPx = startX * width
        val startYPx = startY * height
        val endXPx = endX * width
        val endYPx = endY * height

        // Simulate touch down
        val downTime = System.currentTimeMillis()
        val downEvent = MotionEvent.obtain(
            downTime,
            downTime,
            MotionEvent.ACTION_DOWN,
            startXPx,
            startYPx,
            0
        )
        nativeView.dispatchTouchEvent(downEvent)
        downEvent.recycle()

        // Simulate touch move (optional, for smoother swipe)
        val moveEvent = MotionEvent.obtain(
            downTime,
            downTime + 100,
            MotionEvent.ACTION_MOVE,
            (startXPx + endXPx) / 2,
            (startYPx + endYPx) / 2,
            0
        )
        nativeView.dispatchTouchEvent(moveEvent)
        moveEvent.recycle()

        // Simulate touch up
        val upEvent = MotionEvent.obtain(
            downTime,
            downTime + 200,
            MotionEvent.ACTION_UP,
            endXPx,
            endYPx,
            0
        )
        nativeView.dispatchTouchEvent(upEvent)
        upEvent.recycle()
    }
}
