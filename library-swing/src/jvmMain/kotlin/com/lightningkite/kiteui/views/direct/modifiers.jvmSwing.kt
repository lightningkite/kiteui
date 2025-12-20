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

/**
 * Extension properties to get/set alignment on JComponent
 */
var javax.swing.JComponent.horizontalAlign: Align
    get() = getClientProperty(HORIZONTAL_ALIGN_PROPERTY) as? Align ?: Align.Stretch
    set(value) = putClientProperty(HORIZONTAL_ALIGN_PROPERTY, value)

var javax.swing.JComponent.verticalAlign: Align
    get() = getClientProperty(VERTICAL_ALIGN_PROPERTY) as? Align ?: Align.Stretch
    set(value) = putClientProperty(VERTICAL_ALIGN_PROPERTY, value)

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
        // TODO: Implement size constraints
        // Store constraints for layout
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
