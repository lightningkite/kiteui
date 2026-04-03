@file:Suppress("OPT_IN_USAGE")

package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.reactive.context.*
import kotlinx.cinterop.*
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIRefreshControl
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject
import platform.objc.sel_registerName

@ViewModifierDsl3
actual fun ElementWriter.hintPopover(
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

@ViewModifierDsl3
actual fun ElementWriter.textPopover(message: String): ElementWriter = hintPopover {
    themed(PopoverSemantic).text(message)
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddWeight.weight(amount: Float): ElementWriter.CanAddShownWhen {
    return beforeSetup {
        native.extensionWeight = amount
    }
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddWeight.dynamicWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddShownWhen {
    return beforeSetup {
        native::extensionWeight { amount() }
    }
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddAlignment.align(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight {
    return beforeSetup {
        native.extensionHorizontalAlign = horizontal
        native.extensionVerticalAlign = vertical
    }
}

@ViewModifierDsl3
actual inline fun ElementWriter.CanAddScrolling.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ElementWriter {
    return lazyInjectModifierWriter(setup) {
        ScrollView(context, horizontal = horizontal, vertical = vertical)
    }
}

@ViewModifierDsl3
actual inline fun ElementWriter.CanAddScrolling.__scrollsWithRefreshUncontracted(
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

@ViewModifierDsl3
actual fun ElementWriter.CanAddSizing.sizedBox(constraints: SizeConstraints): ElementWriter.CanAddTheme {
    return beforeSetup { native.extensionSizeConstraints = constraints }
}

@ViewModifierDsl3
actual fun ElementWriter.CanAddSizing.dynamicSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme {
    return beforeSetup {
        reactive {
            native.extensionSizeConstraints = constraints()
            native.informParentOfSizeChange()
        }
    }
}

// End
@ViewModifierDsl3
actual fun ElementWriter.CanAddShownWhen.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddSizing {
    return beforeSetup {
        native.hidden = !default
        var runNumber = 0
        var lastCommitted = 0
        reactive {
            val value = condition()
            val myRun = ++runNumber
//            println("$native Starting run $myRun")
            if (animationsEnabled) {
                if (native.hidden) {
                    native.alpha = 0.0
                    native.hidden = false
//                    println("Set ${this@beforeNextElementSetup.native} .hidden = FALSE forced")
                    native.extensionCollapsed = true
                }
                animateIfAllowed(onComplete = {
                    if (myRun > lastCommitted) {
                        native.hidden = !value
//                        println("Set ${this@beforeNextElementSetup.native} .hidden = ${!value}")
                        native.extensionCollapsed = false
                        native.informParentOfSizeChange()
                        lastCommitted = myRun
//                        println("$native Committed $lastCommitted")
                    } else {
//                        println("Couldn't set ${this@beforeNextElementSetup.native} .hidden = ${!value}  -  $myRun > $lastCommitted")
                    }
                }) {
                    if (!value) native.alpha = 0.0
                    else native.alpha = opacity
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
//                println("$native Committed $lastCommitted")
            }
        }
    }
}
