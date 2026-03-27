@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.EmphasizedSemantic
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.MainContentSemantic
import com.lightningkite.kiteui.models.PopoverPreferredDirection
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.kiteui.views.direct.NumberInput
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.ScrollingBehaviors
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.__scrollsUncontracted
import com.lightningkite.kiteui.views.direct.align
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.subtext
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.onRemove
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.getValue
import kotlin.setValue


@Deprecated("Wrong import; this has moved", ReplaceWith("launch", "com.lightningkite.reactive.launch"), DeprecationLevel.ERROR) val launch = Unit
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope", "com.lightningkite.reactive.context.reactiveScope"), DeprecationLevel.ERROR) val reactiveScope = Unit

@Deprecated("Wrong import; this has moved", ReplaceWith("DropTargetDelegate", "com.lightningkite.kiteui.models.DropTargetDelegate")) typealias DropTargetDelegate = com.lightningkite.kiteui.models.DropTargetDelegate

@Deprecated("Renamed to ElementContext", ReplaceWith("ElementContext")) typealias RContext = ElementContext
@Deprecated("Renamed", ReplaceWith("RContextCommonCode")) typealias RContextHelper = ElementContextCommonCode

@Deprecated("Renamed", ReplaceWith("debugName"))
var Element.testId: String?
    get() = debugName
    set(value) {
        debugName = value
    }

// modifiers

@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup"))
fun ElementWriter.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup"))
fun ElementWriter.CanAddScrolling.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddScrolling = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup"))
fun ElementWriter.CanAddSizing.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddSizing = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup"))
fun ElementWriter.CanAddTheme.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddTheme = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup"))
fun ElementWriter.CanAddShownWhen.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddShownWhen = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup"))
fun ElementWriter.CanAddWeight.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddWeight = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup"))
fun ElementWriter.CanAddAlignment.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddAlignment = beforeSetup(setup)

@Deprecated("No longer supported, set debugName directly on the element", level = DeprecationLevel.ERROR)
operator fun String.minus(writer: ViewWriter): ViewWriter = writer.also {
    it.beforeSetup {
        debugName = this@minus
    }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
fun ElementWriter.CanAddTheme.themeFromLast(calculate: (Theme) -> Theme): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it).withBack } }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
inline fun ElementWriter.CanAddTheme.maybeThemeFromLast(crossinline calculate: (Theme) -> Theme?): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it)?.withBack ?: it.withoutBack } }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
inline fun ElementWriter.CanAddTheme.tweakTheme(crossinline calculate: (Theme) -> Theme): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it).withoutBack } }
}

@Deprecated("Use hintPopover or menuButton depending on your situation.", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
expect fun ElementWriter.hasPopover(
    requiresClick: Boolean = false,
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.() -> Unit
): ElementWriter

@ViewModifierDsl3
@Deprecated("No longer needed - just tell the parent what its spacing value should be.", ReplaceWith("this"), DeprecationLevel.ERROR)
val ViewWriter.marginless: ViewWriter get() = this

@ViewModifierDsl3
@Deprecated("Renamed to 'shownWhen'", ReplaceWith("shownWhen", "com.lightningkite.kiteui.views.direct.shownWhen"))
fun ElementWriter.CanAddShownWhen.onlyWhen(default: Boolean = false, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddTheme = shownWhen(default, condition)

@ViewModifierDsl3
@Deprecated("use align instead", ReplaceWith("align"))
fun ElementWriter.CanAddAlignment.gravity(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight = align(horizontal, vertical)

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
val ElementWriter.CanAddScrolling.scrolls: ElementWriter get() = __scrollsUncontracted(vertical = true, horizontal = false)

@ViewModifierDsl3
@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollingHorizontally"))
val ElementWriter.CanAddScrolling.scrollsHorizontally: ElementWriter get() = __scrollsUncontracted(vertical = false, horizontal = true)

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
inline fun ElementWriter.CanAddScrolling.scrolls(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = false, setup)
}

@ViewModifierDsl3
@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollingHorizontally"))
inline fun ElementWriter.CanAddScrolling.scrollsHorizontally(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = false, horizontal = true, setup)
}

@ViewModifierDsl3
@Deprecated("use scrollingBoth instead", ReplaceWith("scrollingBoth"))
inline fun ElementWriter.CanAddScrolling.scrollsBoth(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = true, setup)
}

@ViewModifierDsl3
@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
inline fun ElementWriter.CanAddScrolling.scrolls(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsUncontracted(vertical = vertical, horizontal = horizontal, setup)
}

// themes

@Deprecated("Use themed() instead", ReplaceWith("themed(this)"))
context(writer: ElementWriter.CanAddTheme)
val Semantic.onNext: ElementWriter.CanAddTheme get() = writer.themed(this)

@Deprecated("Use themed() instead", ReplaceWith("themed(theme)"))
fun ElementWriter.CanAddTheme.onNext(theme: ThemeDerivation): ElementWriter.CanAddTheme = themed(theme)

@ViewModifierDsl3
@Deprecated("Renamed to 'emphasized' for consistency of adjective terms.", ReplaceWith("emphasized", "com.lightningkite.kiteui.views.emphasized"))
inline val ElementWriter.CanAddTheme.emphasize: ElementWriter.CanAddTheme get() = themed(EmphasizedSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DialogSemantic)", "com.lightningkite.kiteui.models.DialogSemantic"))
inline val ElementWriter.CanAddTheme.dialog: ElementWriter.CanAddTheme get() = themed(DialogSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(MainContentSemantic)", "com.lightningkite.kiteui.models.MainContentSemantic"))
inline val ElementWriter.CanAddTheme.mainContent: ElementWriter.CanAddTheme get() = themed(MainContentSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(HoverSemantic)", "com.lightningkite.kiteui.models.HoverSemantic"))
inline val ElementWriter.CanAddTheme.hover: ElementWriter.CanAddTheme get() = themed(HoverSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DownSemantic)", "com.lightningkite.kiteui.models.DownSemantic"))
inline val ElementWriter.CanAddTheme.down: ElementWriter.CanAddTheme get() = themed(DownSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(SelectedSemantic)", "com.lightningkite.kiteui.models.SelectedSemantic"))
inline val ElementWriter.CanAddTheme.selected: ElementWriter.CanAddTheme get() = themed(SelectedSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(UnselectedSemantic)", "com.lightningkite.kiteui.models.UnselectedSemantic"))
inline val ElementWriter.CanAddTheme.unselected: ElementWriter.CanAddTheme get() = themed(UnselectedSemantic)
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DisabledSemantic)", "com.lightningkite.kiteui.models.DisabledSemantic"))
inline val ElementWriter.CanAddTheme.disabled: ElementWriter.CanAddTheme get() = themed(DisabledSemantic)

@ViewModifierDsl3
@Deprecated("Renamed to 'padded'", ReplaceWith("padded", "com.lightningkite.kiteui.views.direct.padded"))
val ElementWriter.CanAddTheme.withDefaultPadding: ElementWriter.CanAddTheme get() = padded


// views

class Label(val label: TextView, val container: RowOrCol): ContainerElement by container {
    var content: String by label::content
}

@OptIn(ExperimentalContracts::class)
@Deprecated("use the new label: label(String, RowOrCol.() -> Unit)")
inline fun ElementWriter.label(setup: Label.() -> Unit = {}): Label {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    val l: Label
    col {
        val label = subtext()
        gap = 0.px
        l = Label(label, this)
        setup(l)
    }
    return l
}

@Deprecated("Use NumberInput instead", ReplaceWith("NumberInput"))
typealias NumberField = NumberInput

@OptIn(ExperimentalContracts::class)
@Deprecated("Use numberInput instead", ReplaceWith("this.numberInput(setup)", "com.lightningkite.kiteui.views.direct.numberInput"))
inline fun ElementWriter.numberField(setup: NumberInput.() -> Unit = {}): NumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(NumberInput(context) , setup)
}

// addons

@Deprecated(
    "Use 'pageNavigator' instead",
    ReplaceWith("this.pageNavigator", "com.lightningkite.kiteui.navigator.pageNavigator"),
    DeprecationLevel.ERROR
)
val ViewWriter.navigator by ViewWriter::pageNavigator

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
val PlatformNavigator: PageNavigator get() = throw NotImplementedError()

@Deprecated("Use directly through context", ReplaceWith("context.safeInsets()"))
var Element.safeInsets
    get() = context.safeInsets
    set(value) { context.safeInsets = value }
@Deprecated("Use directly through context", ReplaceWith("context.popoverParent()"))
var Element.popoverParent
    get() = context.popoverParent
    set(value) { context.popoverParent = value }
@Deprecated("Use directly through context", ReplaceWith("context.popoverCloser()"))
var Element.popoverCloser
    get() = context.popoverCloser
    set(value) { context.popoverCloser = value }
@Deprecated("Use directly through context", ReplaceWith("context.popoverKeepOpen()"))
var Element.popoverKeepOpen
    get() = context.popoverKeepOpen
    set(value) { context.popoverKeepOpen = value }

@Deprecated("Use directly through context", ReplaceWith("context.closePopovers()"))
fun Element.closePopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closePopovers()
}
@Deprecated("Use directly through context", ReplaceWith("context.closeThisPopover()"))
fun Element.closeThisPopover() {
    popoverCloser?.invoke()
    popoverCloser = null
    popoverParent?.closeSiblingPopovers()
}
@Deprecated("Use directly through context", ReplaceWith("context.closeSiblingPopovers()"))
fun Element.closeSiblingPopovers() {
    popoverCloser?.invoke()
    popoverCloser = null
}
@Deprecated("Use directly through context", ReplaceWith("context.keepPopoverOpen()"))
fun Element.keepPopoverOpen(lifecycle: CoroutineScope) {
    popoverKeepOpen++
    lifecycle.onRemove { popoverKeepOpen-- }
}

@Deprecated("Set on native element directly")
var Element.showOnPrint: Boolean
    get() = underlyingNativeElement.showOnPrint
    set(value) { underlyingNativeElement.showOnPrint = value }