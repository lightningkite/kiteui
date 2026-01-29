package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewModifierDsl3
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import javax.swing.JScrollPane

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWriter {
    return beforeNextElementSetup {
        val debug = System.getProperty("kiteui.debug.layout", "false") == "true"
        if (debug) {
            println("  [weight modifier] Setting weight=$amount on ${native?.javaClass?.simpleName}")
        }

        lastSetWeight = amount
        if (native is javax.swing.JComponent) {
            val component = native as javax.swing.JComponent
            component.weight = amount

            // Weighted components need to have their maximum size set to allow expansion
            if (amount > 0f) {
                component.maximumSize = java.awt.Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
            }

            if (debug) {
                println("  [weight modifier] Weight successfully set to ${component.weight}")
            }

            parent?.native?.revalidate()
        } else if (debug) {
            println("  [weight modifier] ERROR: native is not JComponent! It's ${native?.javaClass?.name}")
        }
    }
}

@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: TypedReactiveContext<*>.() -> Float): ViewWriter {
    return beforeNextElementSetup {
        reactiveScope {
            val weight = amount()
            lastSetWeight = weight
            if (native is javax.swing.JComponent) {
                val component = native as javax.swing.JComponent
                component.weight = weight

                // Weighted components need to have their maximum size set to allow expansion
                if (weight > 0f) {
                    component.maximumSize = java.awt.Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
                }

                parent?.native?.revalidate()
            }
        }
    }
}

/**
 * Client property keys for storing alignment on components
 */
private const val HORIZONTAL_ALIGN_PROPERTY = "kiteui.horizontalAlign"
private const val VERTICAL_ALIGN_PROPERTY = "kiteui.verticalAlign"
private const val HORIZONTAL_ALIGN_EXPLICIT = "kiteui.horizontalAlignExplicit"
private const val VERTICAL_ALIGN_EXPLICIT = "kiteui.verticalAlignExplicit"
private const val NEW_CHILD_HORIZONTAL_ALIGN_PROPERTY = "kiteui.newChildHorizontalAlign"
private const val NEW_CHILD_VERTICAL_ALIGN_PROPERTY = "kiteui.newChildVerticalAlign"

/**
 * Extension properties to get/set alignment on JComponent.
 * When getting alignment, checks if explicitly set, otherwise falls back to parent's newChild* alignment, then Stretch.
 */
var javax.swing.JComponent.horizontalAlign: Align
    get() {
        val explicit = getClientProperty(HORIZONTAL_ALIGN_EXPLICIT) as? Boolean ?: false
        if (explicit) {
            return getClientProperty(HORIZONTAL_ALIGN_PROPERTY) as? Align ?: Align.Stretch
        }
        // Fall back to parent's newChildHorizontalAlign if not explicitly set
        val parent = this.parent
        if (parent is javax.swing.JComponent) {
            val parentDefault = parent.newChildHorizontalAlign
            if (parentDefault != null) return parentDefault
        }
        return Align.Stretch
    }
    set(value) {
        putClientProperty(HORIZONTAL_ALIGN_PROPERTY, value)
        putClientProperty(HORIZONTAL_ALIGN_EXPLICIT, true)
    }

var javax.swing.JComponent.verticalAlign: Align
    get() {
        val explicit = getClientProperty(VERTICAL_ALIGN_EXPLICIT) as? Boolean ?: false
        if (explicit) {
            return getClientProperty(VERTICAL_ALIGN_PROPERTY) as? Align ?: Align.Stretch
        }
        // Fall back to parent's newChildVerticalAlign if not explicitly set
        val parent = this.parent
        if (parent is javax.swing.JComponent) {
            val parentDefault = parent.newChildVerticalAlign
            if (parentDefault != null) return parentDefault
        }
        return Align.Stretch
    }
    set(value) {
        putClientProperty(VERTICAL_ALIGN_PROPERTY, value)
        putClientProperty(VERTICAL_ALIGN_EXPLICIT, true)
    }

/**
 * Extension properties for parent containers to set default alignment for new children.
 */
var javax.swing.JComponent.newChildHorizontalAlign: Align?
    get() = getClientProperty(NEW_CHILD_HORIZONTAL_ALIGN_PROPERTY) as? Align
    set(value) = putClientProperty(NEW_CHILD_HORIZONTAL_ALIGN_PROPERTY, value)

var javax.swing.JComponent.newChildVerticalAlign: Align?
    get() = getClientProperty(NEW_CHILD_VERTICAL_ALIGN_PROPERTY) as? Align
    set(value) = putClientProperty(NEW_CHILD_VERTICAL_ALIGN_PROPERTY, value)

/**
 * Helper function to layout a child component within a container respecting its alignment properties.
 * Used by container layout managers (Button, Link, ExternalLink, Frame) to properly center content.
 */
fun layoutChildWithAlignment(
    child: java.awt.Component,
    insets: java.awt.Insets,
    availableWidth: Int,
    availableHeight: Int
) {
    // Get alignment from child component
    val horizontalAlign = if (child is javax.swing.JComponent) child.horizontalAlign else Align.Stretch
    val verticalAlign = if (child is javax.swing.JComponent) child.verticalAlign else Align.Stretch

    // Calculate child size based on alignment
    val prefSize = child.preferredSize
    val childWidth = when (horizontalAlign) {
        Align.Stretch -> availableWidth
        else -> prefSize.width.coerceAtMost(availableWidth)
    }
    val childHeight = when (verticalAlign) {
        Align.Stretch -> availableHeight
        else -> prefSize.height.coerceAtMost(availableHeight)
    }

    // Calculate position based on alignment
    val x = when (horizontalAlign) {
        Align.Start -> insets.left
        Align.Center -> insets.left + (availableWidth - childWidth) / 2
        Align.End -> insets.left + (availableWidth - childWidth)
        Align.Stretch -> insets.left
    }
    val y = when (verticalAlign) {
        Align.Start -> insets.top
        Align.Center -> insets.top + (availableHeight - childHeight) / 2
        Align.End -> insets.top + (availableHeight - childHeight)
        Align.Stretch -> insets.top
    }

    child.setBounds(x, y, childWidth, childHeight)
}

@ViewModifierDsl3
actual fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWriter {
    return beforeNextElementSetup {
        lastSetHorizontalAlign = horizontal
        lastSetVerticalAlign = vertical
        if (native is javax.swing.JComponent) {
            val component = native as javax.swing.JComponent
            component.horizontalAlign = horizontal
            component.verticalAlign = vertical

            // Update component's maximum size to allow stretching
            if (horizontal == Align.Stretch || vertical == Align.Stretch) {
                val maxSize = component.maximumSize
                component.maximumSize = java.awt.Dimension(
                    if (horizontal == Align.Stretch) Int.MAX_VALUE else maxSize.width,
                    if (vertical == Align.Stretch) Int.MAX_VALUE else maxSize.height
                )
            }

            parent?.native?.revalidate()
        }
    }
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWriter {
    return write(ScrollView(context, horizontal, vertical)) {
        ScrollingBehaviorsImpl(native as JScrollPane).apply(setup)
    }
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWriter {
    // TODO: Implement pull-to-refresh for Swing
    return __scrollsUncontracted(vertical, horizontal, setup)
}

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWriter {
    return beforeNextElementSetup {
        // TODO: Implement hint popover using JToolTip
    }
}

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWriter {
    return beforeNextElementSetup {
        // TODO: Implement popover using JPopupMenu
    }
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWriter {
    return beforeNextElementSetup {
        // Set as tooltip
        if (native is javax.swing.JComponent) {
            (native as javax.swing.JComponent).toolTipText = message
        }
    }
}

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: com.lightningkite.kiteui.models.SizeConstraints): ViewWriter {
    return beforeNextElementSetup {
        if (native is javax.swing.JComponent) {
            val component = native as javax.swing.JComponent

            // Apply size constraints using Swing's preferred/minimum/maximum size
            val prefSize = component.preferredSize
            val minSize = component.minimumSize
            val maxSize = component.maximumSize

            // Calculate new dimensions based on constraints
            val newPrefWidth = constraints.width?.value?.toInt() ?: prefSize.width
            val newPrefHeight = constraints.height?.value?.toInt() ?: prefSize.height

            val newMinWidth = constraints.minWidth?.value?.toInt() ?: minSize.width
            val newMinHeight = constraints.minHeight?.value?.toInt() ?: minSize.height

            val newMaxWidth = constraints.maxWidth?.value?.toInt() ?: maxSize.width
            val newMaxHeight = constraints.maxHeight?.value?.toInt() ?: maxSize.height

            // Set preferred size - use explicit width/height if set, otherwise use min as preference if set
            val preferredWidth = constraints.width?.value?.toInt()
                ?: constraints.minWidth?.value?.toInt()
                ?: prefSize.width
            val preferredHeight = constraints.height?.value?.toInt()
                ?: constraints.minHeight?.value?.toInt()
                ?: prefSize.height

            component.preferredSize = java.awt.Dimension(preferredWidth, preferredHeight)
            component.minimumSize = java.awt.Dimension(newMinWidth, newMinHeight)
            component.maximumSize = java.awt.Dimension(newMaxWidth, newMaxHeight)

            parent?.native?.revalidate()
        }
    }
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: com.lightningkite.reactive.context.ReactiveContext.() -> com.lightningkite.kiteui.models.SizeConstraints): ViewWriter {
    return beforeNextElementSetup {
        // TODO: Implement reactive size constraints
    }
}

@ViewModifierDsl3
actual fun ViewWriter.shownWhen(default: Boolean, condition: com.lightningkite.reactive.context.ReactiveContext.() -> Boolean): ViewWriter {
    return beforeNextElementSetup {
        // TODO: Implement reactive visibility
        // For now, just use the default
        native.isVisible = default
    }
}
