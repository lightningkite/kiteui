
@file:Suppress("OPT_IN_USAGE")

package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.ReactiveContext
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.QuartzCore.CALayer
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UILongPressGestureRecognizer
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject
import platform.objc.sel_registerName
import kotlin.time.DurationUnit


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
    return ViewWrapper
}


@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
    beforeNextElementSetup {
        val originalNavigator = screenNavigator
        fun openDialog() {
            dialogScreenNavigator.navigate(object : Screen {
                override fun ViewWriter.render() {
                    dismissBackground {
                        centered - stack {
                            with(split()) {
                                screenNavigator = originalNavigator
                                setup(object : PopoverContext {
                                    override val calculationContext: CalculationContext
                                        get() = this@beforeNextElementSetup

                                    override fun close() {
                                        dialogScreenNavigator.dismiss()
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
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    this.beforeNextElementSetup {
        native.extensionWeight = amount
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: ReactiveContext.() -> Float): ViewWrapper {
    this.beforeNextElementSetup {
        reactiveScope {
            native.extensionWeight = amount()
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        native.extensionHorizontalAlign = horizontal
        native.extensionVerticalAlign = vertical
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper
    get() {
        wrapNextIn(object : RViewWrapper(context) {
            override val native = ScrollLayout()

            init {
                native.horizontal = false
            }
        })
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper
    get() {
        wrapNextIn(object : RViewWrapper(context) {
            override val native = ScrollLayout()

            init {
                native.horizontal = true
            }
        })
        return ViewWrapper
    }

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    beforeNextElementSetup {
        native.extensionSizeConstraints = constraints
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ViewWrapper {
    beforeNextElementSetup {
        reactiveScope {
            native.extensionSizeConstraints = constraints()
            native.informParentOfSizeChange()
        }
    }
    return ViewWrapper
}

// End
@ViewModifierDsl3
actual fun ViewWriter.onlyWhen(default: Boolean, condition: ReactiveContext.() -> Boolean): ViewWrapper {
    beforeNextElementSetup {
        native.hidden = !default
        var runNumber = 0
        var lastCommitted = 0
        reactiveScope {
            val value = condition()
            val myRun = ++runNumber
            if(animationsEnabled) {
                if (native.hidden) {
                    native.alpha = 0.0
                    native.hidden = false
                    native.extensionCollapsed = true
                }
                animateIfAllowed(onComplete = {
                    if(myRun < lastCommitted) {
                        native.hidden = !value
                        native.extensionCollapsed = false
                        lastCommitted = myRun
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
            }
        }
    }
    return ViewWrapper
}
