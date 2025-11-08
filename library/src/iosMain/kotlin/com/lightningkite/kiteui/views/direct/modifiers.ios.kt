@file:Suppress("OPT_IN_USAGE")

package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.*
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UIRefreshControl
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject
import platform.objc.sel_registerName


@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit,
): ViewWrapper {
    beforeNextElementSetup {
        fun openDialog() {
            // TODO
//            toast(inner = setup)
        }

        val actionHolder = object : NSObject() {
            @ObjCAction
            fun eventHandler() = openDialog()
        }
        val rec = UILongPressGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
        native.addGestureRecognizer(rec)
    }
        .let { return it }
}


@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
    beforeNextElementSetup {
        val originalNavigator = pageNavigator
        fun openDialog() {
            dialogPageNavigator.navigate(object : Page {
                override fun ViewWriter.render(): ViewModifiable = run {
                    dismissBackground {
                        centered.frame {
                            with(split()) {
                                pageNavigator = originalNavigator
                                setup(object : PopoverContext {
                                    override val calculationContext: CalculationContext
                                        get() = this@beforeNextElementSetup

                                    override fun close() {
                                        dialogPageNavigator.dismiss()
                                    }
                                })
                            }
                        }
                    }
                }
            })
        }
        if (this is Button) {
            onClick { openDialog() }
        } else {
            val actionHolder = object : NSObject() {
                @ObjCAction
                fun eventHandler() = openDialog()
            }
            val rec = UITapGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
            native.addGestureRecognizer(rec)
        }
    }
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    this.beforeNextElementSetup {
        lastSetWeight = amount
        native.extensionWeight = amount
    }
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWrapper {
    this.beforeNextElementSetup {
        reactiveScope {
            val amount = amount()
            native.extensionWeight = amount
            lastSetWeight = amount
        }
    }
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.align(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        lastSetHorizontalAlign = horizontal
        lastSetVerticalAlign = vertical
        native.extensionHorizontalAlign = horizontal
        native.extensionVerticalAlign = vertical
    }
        .let { return it }
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWrapper {
    return ScrollView(context, horizontal = horizontal, vertical = vertical).apply(setup)
}

@ViewModifierDsl3
actual inline fun ViewWriter.__scrollsWithRefreshUncontracted(
    vertical: Boolean,
    horizontal: Boolean,
    refreshAction: Action,
    crossinline setup: ScrollingBehaviors.() -> Unit
): ViewWrapper {
    val scrollView = ScrollView(context, horizontal = horizontal, vertical = vertical).apply(setup)

    if (vertical) {
        val refreshControl = UIRefreshControl()
        refreshControl.addTarget(
            target = object : NSObject() {
                @ObjCAction
                fun handleRefresh() {
                    refreshAction.startAction(this@__scrollsWithRefreshUncontracted)
                    reactiveScope {
                        refreshAction.state().handle(
                            success = { refreshControl.endRefreshing() },
                            exception = { refreshControl.endRefreshing() },
                            notReady = { refreshControl.beginRefreshing() }
                        )
                    }
                }
            },
            action = sel_registerName("handleRefresh"),
            forControlEvents = UIControlEventValueChanged
        )
        scrollView.scroller.refreshControl = refreshControl
    }

    return scrollView
}

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    beforeNextElementSetup {
        native.extensionSizeConstraints = constraints
    }
        .let { return it }
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWrapper {
    beforeNextElementSetup {
        reactiveScope {
            native.extensionSizeConstraints = constraints()
            native.informParentOfSizeChange()
        }
    }
        .let { return it }
}

// End
@ViewModifierDsl3
actual fun ViewWriter.shownWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ViewWrapper {
    beforeNextElementSetup {
        native.hidden = !default
        var runNumber = 0
        var lastCommitted = 0
        reactiveScope {
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
        .let { return it }
}
