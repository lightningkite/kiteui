@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.reactive.CalculationContext
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.UIKit.UITapGestureRecognizer
import platform.darwin.NSObject
import platform.objc.sel_registerName


//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NLocalTimeField = UIDatePicker
//
//@ViewDsl
//actual inline fun ViewWriter.localTimeFieldActual(crossinline setup: LocalTimeField.() -> Unit): Unit = stack {
//    element(UIDatePicker()){
//        setPreferredDatePickerStyle(UIDatePickerStyle.UIDatePickerStyleCompact)
////        handleTheme(this) { this. = it.foreground.closestColor().toUiColor() }
//        datePickerMode = UIDatePickerMode.UIDatePickerModeTime
//    }
//}
//
//actual var LocalTimeField.action: Action?
//    get() = TODO()
//    set(value) {}
//actual val LocalTimeField.content: Writable<LocalTime?> get() = object: Writable<LocalTime?> {
//    override suspend fun set(value: LocalTime?) {
//        native.date = value?.atDate(1970, 1, 1)?.toNSDateComponents()?.date() ?: NSDate()
//    }
//
//    override suspend fun awaitRaw(): LocalTime? = native.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).time
//
//    override fun addListener(listener: () -> Unit): () -> Unit {
//        return native.onEvent(UIControlEventValueChanged, listener)
//    }
//
//}
//actual inline var LocalTimeField.range: ClosedRange<LocalTime>?
//    get() = TODO()
//    set(value) {
//    }

//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NAutoCompleteTextField = UIView
//
//@ViewDsl
//actual inline fun ViewWriter.autoCompleteTextFieldActual(crossinline setup: AutoCompleteTextField.() -> Unit): Unit =
//    todo("autoCompleteTextField")
//
//actual val AutoCompleteTextField.content: Writable<String> get() = Property("")
//actual inline var AutoCompleteTextField.keyboardHints: KeyboardHints
//    get() = TODO()
//    set(value) {}
//actual var AutoCompleteTextField.action: Action?
//    get() = TODO()
//    set(value) {}
//actual inline var AutoCompleteTextField.suggestions: List<String>
//    get() = TODO()
//    set(value) {}

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper {
    beforeNextElementSetup {
        val originalNavigator = navigator
        fun openDialog() {
            navigator.dialog.navigate(object : KiteUiScreen {
                override fun ViewWriter.render() {
                    dismissBackground {
                        centered - stack {
                            with(split()) {
                                navigator = originalNavigator
                                setup(object : PopoverContext {
                                    override val calculationContext: CalculationContext
                                        get() = this@beforeNextElementSetup.calculationContext
                                    override fun close() {
                                        navigator.dialog.dismiss()
                                    }
                                })
                            }
                        }
                    }
                }
            })
        }
        if(this is NButton) {
            Button(this).onClick { openDialog() }
        } else {
            val actionHolder = object : NSObject() {
                @ObjCAction
                fun eventHandler() = openDialog()
            }
            val rec = UITapGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
            addGestureRecognizer(rec)
        }
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    val parent = this.currentView
    this.beforeNextElementSetup {
        this.extensionWeight = amount
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        extensionHorizontalAlign = horizontal
        extensionVerticalAlign = vertical
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper
    get() {
        wrapNext(ScrollLayout()) {
            horizontal = false
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper
    get() {
        wrapNext(ScrollLayout()) {
            horizontal = true
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    beforeNextElementSetup {
        extensionSizeConstraints = constraints
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual val ViewWriter.padded: ViewWrapper
    get() {
        beforeNextElementSetup {
            extensionForcePadding = true
        }
        return ViewWrapper
    }

@ViewModifierDsl3
actual val ViewWriter.unpadded: ViewWrapper
    get() {
        beforeNextElementSetup {
            extensionForcePadding = false
        }
        return ViewWrapper
    }

// End
@ViewModifierDsl3
actual fun ViewWriter.onlyWhen(default: Boolean, condition: suspend () -> Boolean): ViewWrapper {
    beforeNextElementSetup {
        exists = true
        ::exists.invoke(condition)
    }
    return ViewWrapper
}