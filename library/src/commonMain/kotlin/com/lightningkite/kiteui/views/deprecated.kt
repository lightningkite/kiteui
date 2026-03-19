package com.lightningkite.kiteui.views

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
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.subtext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.getValue
import kotlin.setValue

@Deprecated("Wrong import; this has moved", ReplaceWith("launch", "com.lightningkite.reactive.launch"), DeprecationLevel.ERROR) val launch = Unit
@Deprecated("Wrong import; this has moved", ReplaceWith("reactiveScope", "com.lightningkite.reactive.context.reactiveScope"), DeprecationLevel.ERROR) val reactiveScope = Unit

@Deprecated("Wrong import; this has moved", ReplaceWith("DropTargetDelegate", "com.lightningkite.kiteui.models.DropTargetDelegate")) typealias DropTargetDelegate = com.lightningkite.kiteui.models.DropTargetDelegate

@Deprecated("Renamed", ReplaceWith("RContextCommonCode")) typealias RContextHelper = ElementContextCommonCode

@Deprecated("Renamed", ReplaceWith("debugName"))
var Element.testId: String?
    get() = debugName
    set(value) {
        debugName = value
    }

// modifiers

@Deprecated("No longer supported, set debugName directly on the element", level = DeprecationLevel.ERROR)
operator fun String.minus(writer: ViewWriter): ViewWriter = writer.also {
    it.beforeNextElementSetup {
        debugName = this@minus
    }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
fun ElementWriter.CanAddTheme.themeFromLast(calculate: (Theme) -> Theme): ElementWriter {
    return beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withBack }
    }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
inline fun ElementWriter.CanAddTheme.maybeThemeFromLast(crossinline calculate: (Theme) -> Theme?): ElementWriter {
    return beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it)?.withBack ?: it.withoutBack }
    }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
inline fun ElementWriter.CanAddTheme.tweakTheme(crossinline calculate: (Theme) -> Theme): ElementWriter {
    return beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withoutBack }
    }
}

@Deprecated("Use hintPopover or opensMenu depending on your situation.", level = DeprecationLevel.ERROR)
@ViewModifierDsl3
expect fun ElementWriter.hasPopover(
    requiresClick: Boolean = false,
    preferredDirection: PopoverPreferredDirection = PopoverPreferredDirection.belowRight,
    setup: ViewWriter.() -> Unit
): ElementWriter


// themes

@Deprecated("Use themed() instead", ReplaceWith("themed(this)"))
context(writer: ElementWriter.CanAddTheme)
val Semantic.onNext: ElementWriter.CanAddTheme get() = writer.themed(this)

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