package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.ReactiveContext
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.viewDebugTarget


operator fun ViewWrapper.minus(other: ViewWrapper) = ViewWrapper
operator fun ViewWrapper.contains(other: ViewWrapper): Boolean = true

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
fun ViewWriter.themeFromLast(calculate: (Theme) -> Theme): ViewWrapper {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withBack }
    }
    return ViewWrapper
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
inline fun ViewWriter.maybeThemeFromLast(crossinline calculate: (Theme) -> Theme?): ViewWrapper {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it)?.withBack ?: it.withoutBack }
    }
    return ViewWrapper
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
inline fun ViewWriter.tweakTheme(crossinline calculate: (Theme) -> Theme): ViewWrapper {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withoutBack }
    }
    return ViewWrapper
}

@ViewModifierDsl3
inline val ViewWriter.card: ViewWrapper get() = CardSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.dialog: ViewWrapper get() = DialogSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.mainContent: ViewWrapper get() = MainContentSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.fieldTheme: ViewWrapper get() = FieldSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.buttonTheme: ViewWrapper get() = ButtonSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.hover: ViewWrapper get() = HoverSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.down: ViewWrapper get() = DownSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.selected: ViewWrapper get() = SelectedSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.unselected: ViewWrapper get() = UnselectedSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.disabled: ViewWrapper get() = DisabledSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.bar: ViewWrapper get() = BarSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.nav: ViewWrapper get() = NavSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.important: ViewWrapper get() = ImportantSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.critical: ViewWrapper get() = CriticalSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.warning: ViewWrapper get() = WarningSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.danger: ViewWrapper get() = DangerSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.affirmative: ViewWrapper get() = AffirmativeSemantic.onNext

@ViewModifierDsl3
val ViewWriter.compact: ViewWrapper
    get() = tweakTheme {
        it.copy(
            id = "compact",
            spacing = it.spacing / 2
        )
    }

@ViewModifierDsl3
val ViewWriter.bold: ViewWrapper
    get() = tweakTheme {
        it.copy(
            id = "bold",
            font = it.font.copy(bold = true)
        )
    }

@ViewModifierDsl3
fun ViewWriter.textSize(size: Dimension): ViewWrapper = tweakTheme {
    it.copy(
        font = it.font.copy(size = size)
    )
}

@ViewModifierDsl3
val ViewWriter.italic: ViewWrapper
    get() = tweakTheme {
        it.copy(
            id = "italic",
            font = it.font.copy(italic = true)
        )
    }

@ViewModifierDsl3
val ViewWriter.allCaps: ViewWrapper
    get() = tweakTheme {
        it.copy(
            id = "allCaps",
            font = it.font.copy(allCaps = true)
        )
    }

@ViewModifierDsl3
fun ViewWriter.withSpacing(multiplier: Double): ViewWrapper = tweakTheme { it.copy(spacing = it.spacing * multiplier) }

@ViewModifierDsl3
val ViewWriter.navSpacing: ViewWrapper
    get() {
        beforeNextElementSetup {
            useNavSpacing = true
        }
        return ViewWrapper
    }


fun RView.dynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?) {
    val existing = themeChoice
    reactiveScope {
        themeChoice = existing + (calculate() ?: ThemeDerivation.none)
    }
}