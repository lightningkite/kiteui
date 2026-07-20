package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement
import com.lightningkite.kiteui.views.theme
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.context.reactive
import kotlin.invoke
import kotlin.math.max
import kotlin.math.roundToInt

public abstract class NativeLinearLayoutElement(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    override val native: SlightlyModifiedLinearLayout = SlightlyModifiedLinearLayout(context.activity)

    override var gap: Dimension? = null
        set(value) {
            field = value
            for (child in children) child.underlyingNativeElement.updateCorners()
            native.gap = (value ?: theme.gap).value.roundToInt()
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.gap = (gap ?: theme.theme.gap).value.roundToInt()
    }

    override val spacingForChildCornerRadii: Dimension
        get() = super<LinearLayoutElement>.spacingForChildCornerRadii
}

public actual class RowOrCol actual constructor(context: ElementContext) : NativeLinearLayoutElement(context) {
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        SimplifiedLinearLayout.LayoutParams(
            if (vertical) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
            if (vertical) ViewGroup.LayoutParams.WRAP_CONTENT else ViewGroup.LayoutParams.MATCH_PARENT,
        )

    public actual var vertical: Boolean
        get() = native.orientation == SimplifiedLinearLayout.VERTICAL
        set(value) {
            native.orientation = if (value) SimplifiedLinearLayout.VERTICAL else SimplifiedLinearLayout.HORIZONTAL
            native.gravity = if (value) Gravity.CENTER_HORIZONTAL else Gravity.CENTER_VERTICAL
        }

    public actual fun spacingOverrideBeforeNext(amount: Dimension) {}
}

public actual class RowCollapsingToColumn actual constructor(context: ElementContext, breakpoints: List<Dimension>) : NativeLinearLayoutElement(context) {
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        SimplifiedLinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    init {
        native.orientation = SimplifiedLinearLayout.VERTICAL
        native.gravity = Gravity.CENTER_HORIZONTAL
        reactive {
            val w = AppState.windowInfo().width
            val index = breakpoints.indexOfFirst { w > it }
            if (index == -1 || index % 2 == 1) {
                native.orientation = SimplifiedLinearLayout.VERTICAL
                native.gravity = Gravity.CENTER_HORIZONTAL
                native.ignoreWeights = true
            } else {
                native.orientation = SimplifiedLinearLayout.HORIZONTAL
                native.gravity = Gravity.CENTER_VERTICAL
                native.ignoreWeights = false
            }
        }
    }
}

public actual class RowWrapping actual constructor(context: ElementContext) : NativeContainerElement(context), LinearLayoutElement {
    override val native: FlexboxLayout = FlexboxLayout(context.activity)

    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    actual override var gap: Dimension? = null
        set(value) {
            field = value
            for (child in children) child.underlyingNativeElement.updateCorners()
            native.gap = (value ?: theme.gap).value.roundToInt()
            native.lineGap = (value ?: theme.gap).value.roundToInt()
        }

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.gap = (gap ?: theme.theme.gap).value.roundToInt()
        native.lineGap = (gap ?: theme.theme.gap).value.roundToInt()
    }

    override val spacingForChildCornerRadii: Dimension
        get() = super<LinearLayoutElement>.spacingForChildCornerRadii
}





public open class SlightlyModifiedLinearLayout(context: Context) : SimplifiedLinearLayout(context) {
    override fun generateDefaultLayoutParams(): LayoutParams? {
        if (orientation == HORIZONTAL) {
            return LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else if (orientation == VERTICAL) {
            return LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        return null
    }
}

/**
 * A custom layout that implements flexbox-like wrapping behavior for Android.
 * This is similar to the FlexLayout used in the iOS implementation.
 */
public class FlexboxLayout(context: Context) : ViewGroup(context) {
    public var gap: Int = 0
    public var lineGap: Int = 0

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
