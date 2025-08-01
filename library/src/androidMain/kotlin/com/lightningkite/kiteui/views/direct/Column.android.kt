package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A custom layout that implements flexbox-like wrapping behavior for Android.
 * This is similar to the FlexLayout used in the iOS implementation.
 */
class FlexboxLayout(context: Context) : ViewGroup(context) {
    var gap: Int = 0
    var lineGap: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            else -> {
                var maxWidth = 0
                for (i in 0 until childCount) {
                    val child = getChildAt(i)
                    if (child.visibility != GONE) {
                        measureChild(child, widthMeasureSpec, heightMeasureSpec)
                        maxWidth = max(maxWidth, child.measuredWidth)
                    }
                }
                when (widthMode) {
                    MeasureSpec.AT_MOST -> min(maxWidth, widthSize)
                    else -> maxWidth
                }
            }
        }

        // Measure children and calculate total height with wrapping
        var totalHeight = 0
        var currentLineWidth = 0
        var currentLineHeight = 0
        var isFirstInLine = true

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                // Check if this view needs to wrap to the next line
                if (!isFirstInLine && (currentLineWidth + childWidth > width)) {
                    // Move to next line
                    totalHeight += currentLineHeight + lineGap
                    currentLineWidth = 0
                    currentLineHeight = 0
                    isFirstInLine = true
                }

                // Add view to current line
                if (isFirstInLine) {
                    isFirstInLine = false
                } else {
                    currentLineWidth += gap
                }

                currentLineWidth += childWidth
                currentLineHeight = max(currentLineHeight, childHeight)
            }
        }

        // Add the height of the last line
        if (currentLineHeight > 0) {
            totalHeight += currentLineHeight
        }

        val finalHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(totalHeight, heightSize)
            else -> totalHeight
        }

        setMeasuredDimension(width, finalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        var currentX = 0
        var currentY = 0
        var currentLineHeight = 0
        var isFirstInLine = true

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                // Check if this view needs to wrap to the next line
                if (!isFirstInLine && (currentX + childWidth > width)) {
                    // Move to next line
                    currentY += currentLineHeight + lineGap
                    currentX = 0
                    currentLineHeight = 0
                    isFirstInLine = true
                }

                // Add view to current line
                if (isFirstInLine) {
                    isFirstInLine = false
                } else {
                    currentX += gap
                }

                // Position the view
                child.layout(currentX, currentY, currentX + childWidth, currentY + childHeight)

                currentX += childWidth
                currentLineHeight = max(currentLineHeight, childHeight)
            }
        }
    }

    private fun min(a: Int, b: Int): Int = if (a < b) a else b
}

actual class RowWrapping actual constructor(context: RContext) : RView(context) {
    override val native = FlexboxLayout(context.activity)

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.gap = (value ?: theme.gap).value.roundToInt()
            native.lineGap = (value ?: theme.gap).value.roundToInt()
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        native.gap = (gap ?: theme.theme.gap).value.roundToInt()
        native.lineGap = (gap ?: theme.theme.gap).value.roundToInt()
    }

    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    // The default implementations in RView should work correctly since our FlexboxLayout is a ViewGroup.
    // However, we're overriding them here to make it explicit and to match the iOS implementation pattern.

    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
    }

    override fun internalRemoveChild(index: Int) {
        super.internalRemoveChild(index)
    }

    override fun internalClearChildren() {
        super.internalClearChildren()
    }
}
