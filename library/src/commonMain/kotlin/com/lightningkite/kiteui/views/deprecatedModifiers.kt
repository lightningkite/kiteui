@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)
@file:Suppress("DEPRECATION")
@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.DisabledSemantic
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.EmphasizedSemantic
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.MainContentSemantic
import com.lightningkite.kiteui.models.SelectedSemantic
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.UnselectedSemantic
import com.lightningkite.kiteui.views.direct.ScrollingBehaviors
import com.lightningkite.kiteui.views.direct.__scrollsUncontracted
import com.lightningkite.kiteui.views.direct.align
import com.lightningkite.kiteui.views.direct.padded
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.reactive.context.ReactiveContext

@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddScrolling.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddScrolling = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddSizing.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddSizing = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddTheme.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddTheme = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddShownWhen.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddShownWhen = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddWeight.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddWeight = beforeSetup(setup)
@Deprecated("Renamed to reflect new behavior", ReplaceWith("beforeSetup(setup)"))
public fun ElementWriter.CanAddAlignment.beforeNextElementSetup(setup: Element.() -> Unit): ElementWriter.CanAddAlignment = beforeSetup(setup)

@Deprecated("No longer supported, set debugName directly on the element", level = DeprecationLevel.ERROR)
public operator fun String.minus(writer: ViewWriter): ViewWriter = writer.beforeSetup {
    debugName = this@minus
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
public fun ElementWriter.CanAddTheme.themeFromLast(calculate: (Theme) -> Theme): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it).withBack } }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
public inline fun ElementWriter.CanAddTheme.maybeThemeFromLast(crossinline calculate: (Theme) -> Theme?): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it)?.withBack ?: it.withoutBack } }
}

@Deprecated("Just bind to themeChoice directly", level = DeprecationLevel.ERROR)
public inline fun ElementWriter.CanAddTheme.tweakTheme(crossinline calculate: (Theme) -> Theme): ElementWriter {
    return beforeSetup { themeChoice += ThemeDerivation { calculate(it).withoutBack } }
}

@Deprecated("No longer needed - just tell the parent what its spacing value should be.", ReplaceWith("this"), DeprecationLevel.ERROR)
public val ViewWriter.marginless: ViewWriter get() = this

@Deprecated("Renamed to 'shownWhen'", ReplaceWith("shownWhen", "com.lightningkite.kiteui.views.direct.shownWhen"))
public fun ElementWriter.CanAddShownWhen.onlyWhen(default: Boolean = false, condition: ReactiveContext.() -> Boolean): ElementWriter.CanAddTheme = shownWhen(default, condition = condition)

@Deprecated("use align instead", ReplaceWith("align"))
public fun ElementWriter.CanAddAlignment.gravity(horizontal: Align, vertical: Align): ElementWriter.CanAddWeight = align(horizontal, vertical)

@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
public val ElementWriter.CanAddScrolling.scrolls: ElementWriter get() = __scrollsUncontracted(vertical = true, horizontal = false)

@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollingHorizontally"))
public val ElementWriter.CanAddScrolling.scrollsHorizontally: ElementWriter get() = __scrollsUncontracted(vertical = false, horizontal = true)

@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
public inline fun ElementWriter.CanAddScrolling.scrolls(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = false, setup)
}

@Deprecated("use scrollingHorizontally instead", ReplaceWith("scrollingHorizontally"))
public inline fun ElementWriter.CanAddScrolling.scrollsHorizontally(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = false, horizontal = true, setup)
}

@Deprecated("use scrollingBoth instead", ReplaceWith("scrollingBoth"))
public inline fun ElementWriter.CanAddScrolling.scrollsBoth(crossinline setup: ScrollingBehaviors.() -> Unit): ElementWriter {
    return __scrollsUncontracted(vertical = true, horizontal = true, setup)
}

@Deprecated("use scrolling instead", ReplaceWith("scrolling"))
public inline fun ElementWriter.CanAddScrolling.scrolls(
    vertical: Boolean,
    horizontal: Boolean,
    crossinline setup: ScrollingBehaviors.() -> Unit = {}
): ElementWriter {
    return __scrollsUncontracted(vertical = vertical, horizontal = horizontal, setup)
}

// themes

@Deprecated("Use themed() instead", ReplaceWith("themed(this)"))
public context(writer: ElementWriter.CanAddTheme)
val Semantic.onNext: ElementWriter.CanAddTheme get() = writer.themed(this)

@Deprecated("Use themed() instead", ReplaceWith("themed(theme)"))
public fun ElementWriter.CanAddTheme.onNext(theme: ThemeDerivation): ElementWriter.CanAddTheme = themed(theme)

@Deprecated("Renamed to 'emphasized' for consistency of adjective terms.", ReplaceWith("emphasized", "com.lightningkite.kiteui.views.emphasized"))
public inline val ElementWriter.CanAddTheme.emphasize: ElementWriter.CanAddTheme get() = themed(EmphasizedSemantic)
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DialogSemantic)", "com.lightningkite.kiteui.models.DialogSemantic"))
public inline val ElementWriter.CanAddTheme.dialog: ElementWriter.CanAddTheme get() = themed(DialogSemantic)
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(MainContentSemantic)", "com.lightningkite.kiteui.models.MainContentSemantic"))
public inline val ElementWriter.CanAddTheme.mainContent: ElementWriter.CanAddTheme get() = themed(MainContentSemantic)
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(HoverSemantic)", "com.lightningkite.kiteui.models.HoverSemantic"))
public inline val ElementWriter.CanAddTheme.hover: ElementWriter.CanAddTheme get() = themed(HoverSemantic)
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DownSemantic)", "com.lightningkite.kiteui.models.DownSemantic"))
public inline val ElementWriter.CanAddTheme.down: ElementWriter.CanAddTheme get() = themed(DownSemantic)
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(SelectedSemantic)", "com.lightningkite.kiteui.models.SelectedSemantic"))
public inline val ElementWriter.CanAddTheme.selected: ElementWriter.CanAddTheme get() = themed(SelectedSemantic)
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(UnselectedSemantic)", "com.lightningkite.kiteui.models.UnselectedSemantic"))
public inline val ElementWriter.CanAddTheme.unselected: ElementWriter.CanAddTheme get() = themed(UnselectedSemantic)
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("themed(DisabledSemantic)", "com.lightningkite.kiteui.models.DisabledSemantic"))
public inline val ElementWriter.CanAddTheme.disabled: ElementWriter.CanAddTheme get() = themed(DisabledSemantic)

@Deprecated("Renamed to 'padded'", ReplaceWith("padded", "com.lightningkite.kiteui.views.direct.padded"))
public val ElementWriter.CanAddTheme.withDefaultPadding: ElementWriter.CanAddTheme get() = padded
