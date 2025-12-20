package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import java.awt.Component
import java.awt.Container
import java.awt.Dimension as AwtDimension
import java.awt.LayoutManager2
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Client property key for storing weight on components
 */
private const val WEIGHT_PROPERTY = "kiteui.weight"
private const val GAP_BEFORE_OVERRIDE_PROPERTY = "kiteui.gapBeforeOverride"

/**
 * Extension to get/set weight on JComponent
 */
var JComponent.weight: Float
    get() = getClientProperty(WEIGHT_PROPERTY) as? Float ?: 0f
    set(value) = putClientProperty(WEIGHT_PROPERTY, value)

var JComponent.gapBeforeOverride: Dimension?
    get() = getClientProperty(GAP_BEFORE_OVERRIDE_PROPERTY) as? Dimension
    set(value) = putClientProperty(GAP_BEFORE_OVERRIDE_PROPERTY, value)

/**
 * A layout manager similar to Android's LinearLayout that supports weights and gaps.
 * Arranges components in a single line (horizontal or vertical) with optional weight-based sizing.
 */
class LinearLayoutManager(
    var vertical: Boolean = true,
    var gap: Int = 0,
    var ignoreWeights: Boolean = false
) : LayoutManager2 {

    override fun addLayoutComponent(comp: Component?, constraints: Any?) {
        // No-op: we don't use constraints
    }

    override fun addLayoutComponent(name: String?, comp: Component?) {
        // No-op: we don't use named components
    }

    override fun removeLayoutComponent(comp: Component?) {
        // No-op: component tracking handled by Container
    }

    override fun preferredLayoutSize(parent: Container): AwtDimension {
        return calculateSize(parent, false)
    }

    override fun minimumLayoutSize(parent: Container): AwtDimension {
        return calculateSize(parent, true)
    }

    override fun maximumLayoutSize(target: Container): AwtDimension {
        return AwtDimension(Int.MAX_VALUE, Int.MAX_VALUE)
    }

    override fun getLayoutAlignmentX(target: Container): Float = 0.5f

    override fun getLayoutAlignmentY(target: Container): Float = 0.5f

    override fun invalidateLayout(target: Container) {
        // No cached state to invalidate
    }

    /**
     * Calculate the preferred or minimum size of the container
     */
    private fun calculateSize(parent: Container, minimum: Boolean): AwtDimension {
        val insets = parent.insets
        var primarySize = 0
        var secondarySize = 0
        var visibleCount = 0

        for (i in 0 until parent.componentCount) {
            val child = parent.getComponent(i)
            if (!child.isVisible) continue

            // Check if child has weight (flexbox-like behavior: weighted items have flex-basis: 0)
            val childWeight = if (!ignoreWeights && child is JComponent) child.weight else 0f
            val childSize = if (minimum) child.minimumSize else child.preferredSize

            if (visibleCount > 0) {
                val gapToUse = if (child is JComponent) {
                    child.gapBeforeOverride?.value?.roundToInt() ?: gap
                } else gap
                primarySize += gapToUse
            }

            // Weighted children contribute 0 to preferred size (like flex-basis: 0)
            // They will take space from the "remaining" pool during layout
            if (childWeight > 0f) {
                // Only contribute to secondary axis
                if (vertical) {
                    secondarySize = max(secondarySize, childSize.width)
                } else {
                    secondarySize = max(secondarySize, childSize.height)
                }
            } else {
                // Non-weighted children contribute their full size
                if (vertical) {
                    primarySize += childSize.height
                    secondarySize = max(secondarySize, childSize.width)
                } else {
                    primarySize += childSize.width
                    secondarySize = max(secondarySize, childSize.height)
                }
            }
            visibleCount++
        }

        return if (vertical) {
            AwtDimension(
                secondarySize + insets.left + insets.right,
                primarySize + insets.top + insets.bottom
            )
        } else {
            AwtDimension(
                primarySize + insets.left + insets.right,
                secondarySize + insets.top + insets.bottom
            )
        }
    }

    override fun layoutContainer(parent: Container) {
        val insets = parent.insets
        val availableWidth = parent.width - insets.left - insets.right
        val availableHeight = parent.height - insets.top - insets.bottom

        if (availableWidth <= 0 || availableHeight <= 0) return

        // Debug output for weight testing
        val debugLayout = System.getProperty("kiteui.debug.layout", "false") == "true"
        if (debugLayout) {
            println("=== Layout Debug (${if (vertical) "Column" else "Row"}) ===")
            println("Parent size: ${parent.width} x ${parent.height}")
            println("Available: $availableWidth x $availableHeight")
        }

        val components = (0 until parent.componentCount)
            .map { parent.getComponent(it) }
            .filter { it.isVisible }

        if (components.isEmpty()) return

        // Calculate total weight and non-weighted size
        var totalWeight = 0f
        var usedPrimarySize = 0
        var gapCount = 0

        components.forEach { child ->
            val childWeight = if (!ignoreWeights && child is JComponent) child.weight else 0f
            totalWeight += childWeight

            if (gapCount > 0) {
                val gapToUse = if (child is JComponent) {
                    child.gapBeforeOverride?.value?.roundToInt() ?: gap
                } else gap
                usedPrimarySize += gapToUse
            }
            gapCount++

            if (childWeight == 0f) {
                // Non-weighted children use preferred size
                val prefSize = child.preferredSize
                usedPrimarySize += if (vertical) prefSize.height else prefSize.width
            }
        }

        val availablePrimarySize = if (vertical) availableHeight else availableWidth
        val availableSecondarySize = if (vertical) availableWidth else availableHeight
        val remainingPrimarySize = max(0, availablePrimarySize - usedPrimarySize)

        if (debugLayout) {
            println("Total weight: $totalWeight")
            println("Used primary size: $usedPrimarySize")
            println("Available primary: $availablePrimarySize")
            println("Remaining primary: $remainingPrimarySize")
            println("Children:")
            components.forEachIndexed { i, child ->
                val w = if (child is JComponent) child.weight else 0f
                println("  [$i] weight=$w, prefSize=${child.preferredSize}")
            }
        }

        // Layout children
        var primaryPosition = if (vertical) insets.top else insets.left
        var isFirst = true

        components.forEach { child ->
            // Add gap
            if (!isFirst) {
                val gapToUse = if (child is JComponent) {
                    child.gapBeforeOverride?.value?.roundToInt() ?: gap
                } else gap
                primaryPosition += gapToUse
            }
            isFirst = false

            val childWeight = if (!ignoreWeights && child is JComponent) child.weight else 0f

            val childPrimarySize = if (childWeight > 0f && totalWeight > 0f) {
                // Weighted child gets a share of remaining space
                (remainingPrimarySize * (childWeight / totalWeight)).roundToInt()
            } else {
                // Non-weighted child uses preferred size
                val prefSize = child.preferredSize
                if (vertical) prefSize.height else prefSize.width
            }

            // Get alignment for secondary axis
            val secondaryAlign = if (child is JComponent) {
                if (vertical) child.horizontalAlign else child.verticalAlign
            } else {
                Align.Stretch
            }

            // Calculate secondary size and position based on alignment
            val childSecondarySize = when (secondaryAlign) {
                Align.Stretch -> availableSecondarySize
                else -> {
                    val prefSize = child.preferredSize
                    val preferredSecondary = if (vertical) prefSize.width else prefSize.height
                    preferredSecondary.coerceAtMost(availableSecondarySize)
                }
            }

            val secondaryOffset = when (secondaryAlign) {
                Align.Start -> if (vertical) insets.left else insets.top
                Align.Center -> {
                    val baseOffset = if (vertical) insets.left else insets.top
                    baseOffset + (availableSecondarySize - childSecondarySize) / 2
                }
                Align.End -> {
                    val baseOffset = if (vertical) insets.left else insets.top
                    baseOffset + (availableSecondarySize - childSecondarySize)
                }
                Align.Stretch -> if (vertical) insets.left else insets.top
            }

            // Set child bounds
            if (vertical) {
                child.setBounds(
                    secondaryOffset,
                    primaryPosition,
                    childSecondarySize,
                    childPrimarySize
                )
            } else {
                child.setBounds(
                    primaryPosition,
                    secondaryOffset,
                    childPrimarySize,
                    childSecondarySize
                )
            }

            primaryPosition += childPrimarySize
        }
    }
}

actual class RowOrCol actual constructor(context: RContext) : RView(context) {
    override val native = JPanel()
    private val layoutManager = LinearLayoutManager(vertical = true)

    init {
        native.layout = layoutManager
        // Allow the container to expand to fill available space
        native.maximumSize = java.awt.Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
    }

    actual var vertical: Boolean
        get() = layoutManager.vertical
        set(value) {
            layoutManager.vertical = value
            native.revalidate()
        }

    actual fun spacingOverrideBeforeNext(amount: Dimension) {
        beforeNextElementSetup {
            if (native is JComponent) {
                (native as JComponent).gapBeforeOverride = amount
            }
        }
    }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            layoutManager.gap = (value ?: theme.gap).value.roundToInt()
            native.revalidate()
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        layoutManager.gap = (gap ?: theme.theme.gap).value.roundToInt()
    }
}

actual class RowWrapping actual constructor(context: RContext) : RView(context) {
    override val native = JPanel()

    init {
        // TODO: Implement wrapping layout (could use MigLayout or custom layout manager)
        native.layout = LinearLayoutManager(vertical = false)
    }
}
