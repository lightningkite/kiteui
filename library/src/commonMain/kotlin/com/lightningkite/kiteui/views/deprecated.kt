@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)
@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.UnsafeModifier
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.direct.NumberInput
import com.lightningkite.kiteui.views.direct.alert
import com.lightningkite.kiteui.views.direct.confirmDanger
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.rawPopover
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.getValue
import kotlin.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


@Deprecated("Wrong import; this has moved", ReplaceWith("launch", "com.lightningkite.reactive.launch"), DeprecationLevel.ERROR) public val launch: Unit = Unit
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope", "com.lightningkite.reactive.context.reactiveScope"), DeprecationLevel.ERROR) public val reactiveScope: Unit = Unit

@Deprecated("Wrong import; this has moved", ReplaceWith("DropTargetDelegate", "com.lightningkite.kiteui.models.DropTargetDelegate")) public typealias DropTargetDelegate = com.lightningkite.kiteui.models.DropTargetDelegate

@Deprecated("Renamed to ElementContext", ReplaceWith("ElementContext")) public typealias RContext = ElementContext
@Deprecated("Renamed", ReplaceWith("RContextCommonCode")) public typealias RContextHelper = ElementContextCommonCode

@Deprecated("Renamed", ReplaceWith("debugName"))
public var Element.testId: String?
    get() = debugName
    set(value) {
        debugName = value
    }

@Deprecated("No longer supported")
public var Element.transitionId: String?
    get() = null
    set(value) {}


@Deprecated("Use modifier syntax or `applyDynamicTheme`")
@UnsafeModifier
/**
 * Apply a dynamic theme directly to an [Element] (old version)
 *
 * The new version is [dynamicThemed] (with a 'd') applied as a modifier __outside__ the element:
 *
 * ```kotlin
 * val myTheme: Reactive<ThemeDerivation> = ...
 *
 * // from this
 * frame {
 *    dynamicTheme { myTheme() }
 *    text("hello world")
 * }
 *
 * // to this
 * dynamicThemed { myTheme() }.frame {
 *    text("hello world")
 * }
 * ```
 *
 * This change was made to help encourage safety. Dynamic themes require [themeChoice][Element.themeChoice] to be
 * static at the time they are defined. Also, you can only call `dynamicTheme` once per element.
 *
 * If you change the `themeChoice` after a call to `dynamicTheme` you won't get the result you expect. Similarly,
 * if you call `dynamicTheme` twice on an element you'll get weird bugs. The modifier syntax enforces this contract.
 * If you apply a dynamic theme directly _you_ are responsible to uphold this contract.
 *
 * ```kotlin
 * frame {
 *    themeChoice += CardSemantic
 *    dynamicTheme { myTheme() } // dynamic theme applied
 *
 *    themeChoice += ImportantSemantic // <- Bug!!
 *    dynamicTheme { myTheme2() } // <- Bug!!
 * }
 * ```
 * */
public fun Element.dynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?): Unit = applyDynamicTheme(calculate)


// views

@Deprecated("Use NumberInput instead", ReplaceWith("NumberInput"))
public typealias NumberField = NumberInput

@OptIn(ExperimentalContracts::class)
@Deprecated("Use numberInput instead", ReplaceWith("this.numberInput(setup)", "com.lightningkite.kiteui.views.direct.numberInput"))
public inline fun ElementWriter.numberField(setup: NumberInput.() -> Unit = {}): NumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(NumberInput(context) , setup)
}

// addons

@Deprecated(
    "Use 'pageNavigator' instead",
    ReplaceWith("this.pageNavigator", "com.lightningkite.kiteui.navigator.pageNavigator"),
    DeprecationLevel.ERROR
)
public val ViewWriter.navigator: PageNavigator by ViewWriter::pageNavigator

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
public val PlatformNavigator: PageNavigator get() = throw NotImplementedError()

@Deprecated("Use directly through context", ReplaceWith("context.safeInsets()"))
public var Element.safeInsets: Reactive<Edges>
    get() = context.safeInsets
    set(value) { context.safeInsets = value }
@Deprecated("Use the popover directly", ReplaceWith("context.currentPopover?.close()"))
public fun Element.closePopovers() {
    context.currentPopover?.close()
}
@Deprecated("Use the popover directly", ReplaceWith("context.enclosingPopover?.close()"))
public fun Element.closeThisPopover() {
    context.enclosingPopover?.close()
}
@Suppress("DEPRECATION")
@Deprecated("Use the popover directly", ReplaceWith("context.enclosingPopover?.child?.close()"))
public fun Element.closeSiblingPopovers(): Unit = context.closeSiblingPopovers()
@Deprecated("Popovers now stay open while one opened from inside them is.")
public fun Element.keepPopoverOpen(@Suppress("UNUSED_PARAMETER") lifecycle: CoroutineScope) {}

@Deprecated("Use `themeBase` directly", ReplaceWith("themeBase = NativeElementCommonCode.GetBaseTheme.fromParentNonCascading"))
public var NativeElement.themeTakeNonCascadingFromParent: Boolean
    get() = themeBase === NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
    set(value) {
        themeBase =
            if (value) NativeElementCommonCode.GetBaseTheme.fromParentNonCascading
            else NativeElementCommonCode.GetBaseTheme.fromParent
    }

@Deprecated("use directly through context", ReplaceWith("context.overlay(modal, transition, content)"))
public fun ElementWriter.overlayWriter(
    modal: Boolean,
    transition: ScreenTransitions,
    content: ContainerElement.(close: ()->Unit) -> Unit
): Unit = context.overlay(modal = modal, navClosable = modal, transition = transition, body = content)

@Deprecated("use directly through context", ReplaceWith("context.toast(text, duration)")) public fun ElementWriter.toast(text: String, duration: Duration = 3.seconds): Unit = context.toast(text, duration)
@Deprecated("use directly through context", ReplaceWith("context.toast(duration, content)")) public fun ElementWriter.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit): Unit = context.toast(duration, content)
@Deprecated("use directly through context", ReplaceWith("context.dialog(dismissable, content)")) public fun ElementWriter.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddTheme.(close: ()->Unit) -> Unit): Unit = context.dialog(dismissable, content)
@Deprecated("use directly through context", ReplaceWith("context.rawPopover(transition, content)")) public fun ElementWriter.rawPopover(transition: ScreenTransitions, content: ElementWriter.() -> Unit): Unit = context.rawPopover(transition, content = content)

@Deprecated("use directly through context", ReplaceWith("context.toast(text, duration)")) public fun Element.toast(text: String, duration: Duration = 3.seconds): Unit = context.toast(text, duration)
@Deprecated("use directly through context", ReplaceWith("context.toast(duration, content)")) public fun Element.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit): Unit = context.toast(duration, content)
@Deprecated("use directly through context", ReplaceWith("context.dialog(dismissable, content)")) public fun Element.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddTheme.(close: ()->Unit) -> Unit): Unit = context.dialog(dismissable, content)
@Deprecated("use directly through context", ReplaceWith("context.rawPopover(transition, content)")) public fun Element.rawPopover(transition: ScreenTransitions, content: ElementWriter.() -> Unit): Unit = context.rawPopover(transition, content = content)

@Deprecated("use directly through context", ReplaceWith("context.toast(text, duration)")) public fun ContainerElement.toast(text: String, duration: Duration = 3.seconds): Unit = context.toast(text, duration)
@Deprecated("use directly through context", ReplaceWith("context.toast(duration, content)")) public fun ContainerElement.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit): Unit = context.toast(duration, content)
@Deprecated("use directly through context", ReplaceWith("context.dialog(dismissable, content)")) public fun ContainerElement.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddTheme.(close: ()->Unit) -> Unit): Unit = context.dialog(dismissable, content)
@Deprecated("use directly through context", ReplaceWith("context.rawPopover(transition, content)")) public fun ContainerElement.rawPopover(transition: ScreenTransitions, content: ElementWriter.() -> Unit): Unit = context.rawPopover(transition, content = content)

@Deprecated("use directly through context", ReplaceWith("context.confirmDanger(title, body, actionName, cancelName, action)"))
public fun ElementWriter.confirmDanger(
    title: String,
    body: String,
    actionName: String = "OK",
    cancelName: String = "Cancel",
    action: suspend () -> Unit
): Unit = context.confirmDanger(title, body, actionName, cancelName, action)

@Deprecated("use directly through context", ReplaceWith("context.alert(title, body)"))
public fun ElementWriter.alert(
    title: String,
    body: String,
): Unit = context.alert(title, body)


@Deprecated("Renamed to reflect change in receiver", ReplaceWith("contextAddon(init)"))
public fun <T> rContextAddon(init: T): ContextAddon<T> = contextAddon(init)

@Deprecated("Renamed to reflect change in receiver", ReplaceWith("lazyContextAddon(init)"))
public fun <T> rContextAddonGenerate(init: (ElementContext) -> T): ContextAddon<T> = lazyContextAddon(init)

@Deprecated("Renamed to reflect change in receiver", ReplaceWith("lateInitContextAddon()"))
public fun <T> rContextAddonInit(): ContextAddon<T> = lateInitContextAddon<T>()