package com.lightningkite.kiteui.views


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
fun ViewWriter.themeFromLast(calculate: (Theme) -> Theme): ViewWriter {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withBack }
    }.let { return it }
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
inline fun ViewWriter.maybeThemeFromLast(crossinline calculate: (Theme) -> Theme?): ViewWriter {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it)?.withBack ?: it.withoutBack }
    }.let { return it }
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
inline fun ViewWriter.tweakTheme(crossinline calculate: (Theme) -> Theme): ViewWriter {
    beforeNextElementSetup {
        themeChoice += ThemeDerivation { calculate(it).withoutBack }
    }.let { return it }
}

@ViewModifierDsl3
inline val ViewWriter.group: ViewWriter get() = GroupSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.card: ViewWriter get() = CardSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.fieldTheme: ViewWriter get() = FieldSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.buttonTheme: ViewWriter get() = ButtonSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.bar: ViewWriter get() = BarSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.nav: ViewWriter get() = NavSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.important: ViewWriter get() = ImportantSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.critical: ViewWriter get() = CriticalSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.warning: ViewWriter get() = WarningSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.danger: ViewWriter get() = DangerSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.affirmative: ViewWriter get() = AffirmativeSemantic.onNext
@ViewModifierDsl3
inline val ViewWriter.emphasized: ViewWriter get() = EmphasizedSemantic.onNext

@ViewModifierDsl3
@Deprecated("Renamed to 'emphasized' for consistency of adjective terms.", ReplaceWith("emphasized", "com.lightningkite.kiteui.views.emphasized"))
inline val ViewWriter.emphasize: ViewWriter get() = EmphasizedSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("DialogSemantic.onNext", "com.lightningkite.kiteui.models.DialogSemantic"))
inline val ViewWriter.dialog: ViewWriter get() = DialogSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("MainContentSemantic.onNext", "com.lightningkite.kiteui.models.MainContentSemantic"))
inline val ViewWriter.mainContent: ViewWriter get() = MainContentSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("HoverSemantic.onNext", "com.lightningkite.kiteui.models.HoverSemantic"))
inline val ViewWriter.hover: ViewWriter get() = HoverSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("DownSemantic.onNext", "com.lightningkite.kiteui.models.DownSemantic"))
inline val ViewWriter.down: ViewWriter get() = DownSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("SelectedSemantic.onNext", "com.lightningkite.kiteui.models.SelectedSemantic"))
inline val ViewWriter.selected: ViewWriter get() = SelectedSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("UnselectedSemantic.onNext", "com.lightningkite.kiteui.models.UnselectedSemantic"))
inline val ViewWriter.unselected: ViewWriter get() = UnselectedSemantic.onNext
@ViewModifierDsl3
@Deprecated("Use the semantic directly, as this should be uncommon in use.", ReplaceWith("DisabledSemantic.onNext", "com.lightningkite.kiteui.models.DisabledSemantic"))
inline val ViewWriter.disabled: ViewWriter get() = DisabledSemantic.onNext

@ViewModifierDsl3
val ViewWriter.compact: ViewWriter
    get() = CompactSemantic.onNext

@ViewModifierDsl3
val ViewWriter.bold: ViewWriter
    get() = ThemeDerivation {
        it.copy(
            id = "bold",
            font = it.font.copy(bold = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
fun ViewWriter.textSize(size: Dimension): ViewWriter = ThemeDerivation {
    it.copy(
        id = "textSize${size.value.toString().filter { it.isLetterOrDigit() }}",
        font = it.font.copy(size = size)
    ).withoutBack
}.onNext

@ViewModifierDsl3
val ViewWriter.italic: ViewWriter
    get() = ThemeDerivation {
        it.copy(
            id = "italic",
            font = it.font.copy(italic = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
val ViewWriter.allCaps: ViewWriter
    get() = ThemeDerivation {
        it.copy(
            id = "allCaps",
            font = it.font.copy(allCaps = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
val ViewWriter.strikethrough: ViewWriter
    get() = ThemeDerivation {
        it.copy(
            id = "strikethrough",
            font = it.font.copy(strikethrough = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
val ViewWriter.underline: ViewWriter
    get() = ThemeDerivation {
        it.copy(
            id = "underline",
            font = it.font.copy(underline = true)
        ).withoutBack
    }.onNext

@ViewModifierDsl3
fun ViewWriter.withSpacing(multiplier: Double): ViewWriter = ThemeDerivation { it.copy(id = "withgap${multiplier.toString().replace('.', '_')}", gap = it.gap * multiplier).withoutBack }.onNext


fun RView.dynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?) {
    val existing = themeChoice
    reactiveScope {
        themeChoice = existing + (calculate() ?: None)
    }
}