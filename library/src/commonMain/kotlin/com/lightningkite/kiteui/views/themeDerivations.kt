package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


public operator fun ViewWrapper.minus(other: ViewWrapper): ViewWrapper.Companion = ViewWrapper
public operator fun ViewWrapper.contains(other: ViewWrapper): Boolean = true

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
public fun ViewWriter.themeFromLast(calculate: (Theme) -> Theme): ViewWrapper {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withBack }
    }
    return ViewWrapper
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
public inline fun ViewWriter.maybeThemeFromLast(crossinline calculate: (Theme) -> Theme?): ViewWrapper {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it)?.withBack ?: it.withoutBack }
    }
    return ViewWrapper
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
public inline fun ViewWriter.tweakTheme(crossinline calculate: (Theme) -> Theme): ViewWrapper {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withoutBack }
    }
    return ViewWrapper
}

@ViewModifierDsl3
public inline val ViewWriter.card: ViewWrapper get() = CardSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.fieldTheme: ViewWrapper get() = FieldSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.buttonTheme: ViewWrapper get() = ButtonSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.bar: ViewWrapper get() = BarSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.nav: ViewWrapper get() = NavSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.important: ViewWrapper get() = ImportantSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.critical: ViewWrapper get() = CriticalSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.warning: ViewWrapper get() = WarningSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.danger: ViewWrapper get() = DangerSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.affirmative: ViewWrapper get() = AffirmativeSemantic.onNext
@ViewModifierDsl3
public inline val ViewWriter.emphasized: ViewWrapper get() = EmphasizedSemantic.onNext

@ViewModifierDsl3
@Deprecated("Renamed to 'emphasized' for consistency of adjective terms.", ReplaceWith("emphasized", "com.lightningkite.kiteui.views.emphasized"))
public inline val ViewWriter.emphasize: ViewWrapper get() = EmphasizedSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("DialogSemantic.onNext", "com.lightningkite.kiteui.models.DialogSemantic"))
public inline val ViewWriter.dialog: ViewWrapper get() = DialogSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("MainContentSemantic.onNext", "com.lightningkite.kiteui.models.MainContentSemantic"))
public inline val ViewWriter.mainContent: ViewWrapper get() = MainContentSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("HoverSemantic.onNext", "com.lightningkite.kiteui.models.HoverSemantic"))
public inline val ViewWriter.hover: ViewWrapper get() = HoverSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("DownSemantic.onNext", "com.lightningkite.kiteui.models.DownSemantic"))
public inline val ViewWriter.down: ViewWrapper get() = DownSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("SelectedSemantic.onNext", "com.lightningkite.kiteui.models.SelectedSemantic"))
public inline val ViewWriter.selected: ViewWrapper get() = SelectedSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("UnselectedSemantic.onNext", "com.lightningkite.kiteui.models.UnselectedSemantic"))
public inline val ViewWriter.unselected: ViewWrapper get() = UnselectedSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("DisabledSemantic.onNext", "com.lightningkite.kiteui.models.DisabledSemantic"))
public inline val ViewWriter.disabled: ViewWrapper get() = DisabledSemantic.onNext

@ViewModifierDsl3
public val ViewWriter.compact: ViewWrapper
    get() = CompactSemantic.onNext

@ViewModifierDsl3
public val ViewWriter.bold: ViewWrapper
    get() = ThemeDerivation {
        it.copy(
            id = "bold",
            font = it.font.copy(bold = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
public fun ViewWriter.textSize(size: Dimension): ViewWrapper = ThemeDerivation {
    it.copy(
        id = "textSize${size.value.toString().filter { it.isLetterOrDigit() }}",
        font = it.font.copy(size = size)
    ).withoutBack
}.onNext

@ViewModifierDsl3
public val ViewWriter.italic: ViewWrapper
    get() = ThemeDerivation {
        it.copy(
            id = "italic",
            font = it.font.copy(italic = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
public val ViewWriter.allCaps: ViewWrapper
    get() = ThemeDerivation {
        it.copy(
            id = "allCaps",
            font = it.font.copy(allCaps = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
public val ViewWriter.strikethrough: ViewWrapper
    get() = ThemeDerivation {
        it.copy(
            id = "strikethrough",
            font = it.font.copy(strikethrough = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
public val ViewWriter.underline: ViewWrapper
    get() = ThemeDerivation {
        it.copy(
            id = "underline",
            font = it.font.copy(underline = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
public fun ViewWriter.withSpacing(multiplier: Double): ViewWrapper = ThemeDerivation { it.copy(gap = it.gap * multiplier).withoutBack }.onNext


public fun RView.dynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?) {
    val existing = themeChoice
    reactiveScope {
        themeChoice = existing + (calculate() ?: ThemeDerivation.none)
    }
}