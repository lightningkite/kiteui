@file:Suppress("OPT_IN_USAGE")

package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.reactive.context.*
import kotlinx.cinterop.*
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIRefreshControl
import platform.UIKit.UITapGestureRecognizer
import platform.UIKit.accessibilityElementsHidden
import platform.UIKit.accessibilityTraits
import platform.darwin.NSObject
import platform.objc.sel_registerName

public actual fun ElementWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit,
): ElementWriter {
    return beforeSetup {
        fun openDialog() {
            // TODO: implement popover
            // toast(inner = setup)
        }

        val actionHolder = object : NSObject() {
            @ObjCAction
            fun eventHandler() = openDialog()
        }
        val rec = UILongPressGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
        native.addGestureRecognizer(rec)
    }
}

public actual fun ElementWriter.textPopover(message: String): ElementWriter = hintPopover {
    themed(PopoverSemantic).text(message)
}

public actual fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddListElementModifier {
    return beforeSetup {
        native.extensionWeight = amount
    }
}

public actual fun ElementWriter.CanAddWeight.dynamicWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddListElementModifier {
    return beforeSetup {
        native::extensionWeight { amount() }
    }
}

public actual fun ElementWriter.CanAddAlignment.align(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight {
    return beforeSetup {
        native.extensionHorizontalAlign = horizontal
        native.extensionVerticalAlign = vertical
    }
}

public actual inline fun ElementWriter.CanAddScrolling.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ElementWriter {
    return lazyInjectModifierWriter(setup) {
        ScrollView(context, horizontal = horizontal, vertical = vertical)
    }
}

public actual inline fun ElementWriter.CanAddScrolling.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ElementWriter = lazyInjectModifierWriter(setup) {
    val scrollView = ScrollView(context, horizontal = horizontal, vertical = vertical)

    if (vertical) {
        val refreshControl = UIRefreshControl()
        // Single reactive scope tied to scrollView's lifecycle — not recreated on every pull
        scrollView.reactiveScope {
            refreshAction.state().handle(
                success = { refreshControl.endRefreshing() },
                exception = { refreshControl.endRefreshing() },
                // Guard against double-calling: UIKit already adjusts contentOffset when the
                // user initiates a pull. Calling beginRefreshing() again would double-adjust
                // contentOffset.y, leaving a permanent gap at the top after endRefreshing().
                notReady = { if (!refreshControl.refreshing) refreshControl.beginRefreshing() }
            )
        }
        val target = object : NSObject() {
            @ObjCAction
            fun handleRefresh() {
                refreshAction.startAction(this@__scrollsWithRefreshUncontracted)
            }
        }
        refreshControl.addTarget(
            target = target,
            action = sel_registerName("handleRefresh"),
            forControlEvents = UIControlEventValueChanged
        )
        scrollView.scroller.refreshControl = refreshControl
        // UIControl stores targets as weak references; hold a strong ref to prevent GC
        scrollView.tag = target
    }

    scrollView
}

public actual fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddTheme {
    return beforeSetup { native.extensionSizeConstraints = constraints }
}

public actual fun ElementWriter.CanAddSizing.dynamicSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme {
    return beforeSetup {
        reactive {
            native.extensionSizeConstraints = constraints()
            native.informParentOfSizeChange()
        }
    }
}

// End
public actual fun ElementWriter.CanAddShownWhen.shownWhen(default: Boolean, transition: ScreenTransition, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddSizing {
    return beforeSetup {
        native.hidden = !default
        var runNumber = 0
        var lastCommitted = 0
        reactive {
            val value = condition()
            val myRun = ++runNumber
            if (animationsEnabled) {
                if (native.hidden) {
                    native.alpha = 0.0
                    native.hidden = false
                    native.extensionCollapsed = true
                    // Apply visual entry transform if showing
                    if (value && (transition.entryTransform != Transformation() || transition.fade)) {
                        native.transform = transition.entryTransform.toCGAffineTransform(native)
                    }
                }
                animateIfAllowed(onComplete = {
                    if (myRun > lastCommitted) {
                        native.hidden = !value
                        native.extensionCollapsed = false
                        // Reset transform when animation completes
                        if (value) native.transform = platform.CoreGraphics.CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                        native.informParentOfSizeChange()
                        lastCommitted = myRun
                    }
                }) {
                    if (!value) {
                        native.alpha = 0.0
                        // Apply visual exit transform
                        if (transition.exitTransform != Transformation() || transition.fade) {
                            native.transform = transition.exitTransform.toCGAffineTransform(native)
                            if (transition.fade) native.alpha = 0.0
                        }
                    } else {
                        native.alpha = opacity
                        // Animate to identity transform
                        native.transform = platform.CoreGraphics.CGAffineTransformMake(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
                    }
                    native.extensionCollapsed = !value
                    native.informParentOfSizeChange()
                    native.superview?.layoutIfNeeded()
                }
            } else {
                native.extensionCollapsed = false
                native.alpha = opacity
                native.hidden = !value
                native.informParentOfSizeChange()
                lastCommitted = myRun
            }
        }
    }
}

public actual fun ElementWriter.CanAddTheme.asHeading(level: Int): ElementWriter.CanAddTheme =
    beforeSetup { native.accessibilityTraits = native.accessibilityTraits or UIAccessibilityTraitHeader }

public actual val ElementWriter.CanAddTheme.asMain: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asNavigation: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asBanner: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asContentInfo: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asComplementary: ElementWriter.CanAddTheme get() = this
public actual val ElementWriter.CanAddTheme.asSearch: ElementWriter.CanAddTheme get() = this

public actual val ElementWriter.CanAddTheme.asPresentation: ElementWriter.CanAddTheme get() =
    beforeSetup { native.accessibilityElementsHidden = true }

public actual val ElementWriter.CanAddTheme.asList: ElementWriter.CanAddTheme get() = this

public actual val ElementWriter.CanAddListElementModifier.asListItem: ElementWriter.CanAddListElementModifier get() = this

@InternalKiteUi
internal actual fun ContainerElement.setupAsListContainer() {} // VoiceOver infers list structure from content
