package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.ScrollableMouseTransparentPanel
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.px
import java.awt.Component
import java.awt.Container
import java.awt.Dimension as AwtDimension
import java.awt.LayoutManager
import kotlin.math.roundToInt

actual class ProgrammaticLayout actual constructor(context: RContext) : RView(context) {
    override val native = ScrollableMouseTransparentPanel().apply {
        layout = ProgrammaticLayoutManager()
    }

    private val layoutManager get() = native.layout as ProgrammaticLayoutManager

    actual var delegate: ProgrammaticLayoutDelegate
        get() = layoutManager.delegate
        set(value) {
            layoutManager.delegate = value
            invalidateLayout()
        }

    actual fun invalidateLayout() {
        native.revalidate()
    }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            layoutManager.spacingCurrentPx = (gap ?: theme.gap).px
            invalidateLayout()
        }

    override fun refreshPadding() {
        val value = appliedPadding
        layoutManager.paddingTopCurrentPx = value.top.px
        layoutManager.paddingLeftCurrentPx = value.left.px
        layoutManager.paddingRightCurrentPx = value.right.px
        layoutManager.paddingBottomCurrentPx = value.bottom.px
        invalidateLayout()
    }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        layoutManager.spacingCurrentPx = (gap ?: theme.theme.gap).px
    }

    private inner class ProgrammaticLayoutManager : LayoutManager {
        var spacingCurrentPx: Double = 0.0
        var paddingTopCurrentPx: Double = 0.0
        var paddingLeftCurrentPx: Double = 0.0
        var paddingRightCurrentPx: Double = 0.0
        var paddingBottomCurrentPx: Double = 0.0
        private var currentSize: Size = Size.Zero

        var delegate: ProgrammaticLayoutDelegate = ProgrammaticLayoutDelegate.AllFull

        private val inProgress = object : ProgrammingLayoutInProgress {
            override val within: Size
                get() = currentSize
            override val gap: Double get() = spacingCurrentPx
            override val padding: Double get() = paddingLeftCurrentPx
            override val paddingTop: Double get() = paddingTopCurrentPx
            override val paddingLeft: Double get() = paddingLeftCurrentPx
            override val paddingRight: Double get() = paddingRightCurrentPx
            override val paddingBottom: Double get() = paddingBottomCurrentPx

            override fun measure(child: RView, sizeConstraint: Size): Size {
                val nativeChild = child.native
                val prefSize = nativeChild.preferredSize

                // Constrain to the size constraint
                val width = prefSize.width.toDouble().coerceAtMost(sizeConstraint.width)
                val height = prefSize.height.toDouble().coerceAtMost(sizeConstraint.height)

                return Size(width, height)
            }

            override fun place(child: RView, left: Double, top: Double, right: Double, bottom: Double) {
                placed += child.native
                child.native.setBounds(
                    left.roundToInt(),
                    top.roundToInt(),
                    (right - left).roundToInt(),
                    (bottom - top).roundToInt()
                )
            }

            override fun existingPosition(child: RView): Rect {
                val bounds = child.native.bounds
                return Rect(
                    bounds.x.toDouble(),
                    bounds.y.toDouble(),
                    (bounds.x + bounds.width).toDouble(),
                    (bounds.y + bounds.height).toDouble()
                )
            }
        }

        private val placed = HashSet<Component>()

        override fun addLayoutComponent(name: String?, comp: Component?) {
            // No-op
        }

        override fun removeLayoutComponent(comp: Component?) {
            // No-op
        }

        override fun preferredLayoutSize(parent: Container): AwtDimension {
            val availableWidth = parent.width.toDouble().let { if (it > 0) it else 10000.0 }
            val availableHeight = parent.height.toDouble().let { if (it > 0) it else 10000.0 }
            val s = Size(availableWidth, availableHeight)
            currentSize = s

            val r = delegate.measure(this@ProgrammaticLayout, inProgress, s)
            return AwtDimension(r.width.roundToInt(), r.height.roundToInt())
        }

        override fun minimumLayoutSize(parent: Container): AwtDimension {
            // Return a reasonable minimum size
            return AwtDimension(0, 0)
        }

        override fun layoutContainer(parent: Container) {
            val width = parent.width
            val height = parent.height

            if (width == 0 || height == 0) return

            placed.clear()
            val s = Size(width.toDouble(), height.toDouble())
            currentSize = s

            delegate.layout(this@ProgrammaticLayout, inProgress, s)

            // Force layout any children that weren't explicitly placed
            parent.components.forEach { component ->
                if (component !in placed) {
                    // Keep existing bounds for unplaced children
                    val bounds = component.bounds
                    if (bounds.width == 0 || bounds.height == 0) {
                        val prefSize = component.preferredSize
                        component.setBounds(0, 0, prefSize.width, prefSize.height)
                    }
                }
            }
        }
    }
}
