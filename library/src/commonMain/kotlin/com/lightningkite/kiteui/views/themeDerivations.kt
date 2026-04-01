package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.ElementWriter.CanAddTheme
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.ReactiveMutableList

private class ThemedWriter(
    val base: CanAddTheme,
    val theme: ThemeDerivation
) : ElementWriter.CanAddTheme by base {
    @OverrideOnly
    override fun willAddChild(element: Element) {
        base.willAddChild(element)
        element.themeChoice += theme
    }
}

val ElementContext.localDynamicThemeCalculations: ReactiveMutableList<ReactiveContext.() -> ThemeDerivation?> by ContextAddon.Local { ReactiveMutableList() }

fun Element.dynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?) {
    val dynamic = context.localDynamicThemeCalculations
    val first = dynamic.isEmpty()
    dynamic.add(calculate)

    if (first) {
        val existing = themeChoice
        reactive {
            themeChoice = existing + dynamic().fold(ThemeDerivation.None as ThemeDerivation) { acc, t ->
                acc + (t() ?: return@fold acc)
            }
        }
    }
}

@ViewModifierDsl3
fun CanAddTheme.themed(theme: ThemeDerivation): CanAddTheme = ThemedWriter(this, theme)

@ViewModifierDsl3
fun ElementWriter.CanAddDynamicTheme.themed(calculate: ReactiveContext.() -> ThemeDerivation?): ElementWriter.CanAddDynamicTheme {
    return beforeSetup { dynamicTheme(calculate) }
}

@ViewModifierDsl3
inline val CanAddTheme.group: CanAddTheme get() = themed(GroupSemantic)
@ViewModifierDsl3
inline val CanAddTheme.card: CanAddTheme get() = themed(CardSemantic)
@ViewModifierDsl3
inline val CanAddTheme.fieldTheme: CanAddTheme get() = themed(FieldSemantic)
@ViewModifierDsl3
inline val CanAddTheme.buttonTheme: CanAddTheme get() = themed(ButtonSemantic)
@ViewModifierDsl3
inline val CanAddTheme.bar: CanAddTheme get() = themed(BarSemantic)
@ViewModifierDsl3
inline val CanAddTheme.nav: CanAddTheme get() = themed(NavSemantic)
@ViewModifierDsl3
inline val CanAddTheme.important: CanAddTheme get() = themed(ImportantSemantic)
@ViewModifierDsl3
inline val CanAddTheme.critical: CanAddTheme get() = themed(CriticalSemantic)
@ViewModifierDsl3
inline val CanAddTheme.warning: CanAddTheme get() = themed(WarningSemantic)
@ViewModifierDsl3
inline val CanAddTheme.danger: CanAddTheme get() = themed(DangerSemantic)
@ViewModifierDsl3
inline val CanAddTheme.affirmative: CanAddTheme get() = themed(AffirmativeSemantic)
@ViewModifierDsl3
inline val CanAddTheme.emphasized: CanAddTheme get() = themed(EmphasizedSemantic)
@ViewModifierDsl3
inline val CanAddTheme.compact: CanAddTheme get() = themed(CompactSemantic)

@ViewModifierDsl3
val CanAddTheme.bold: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "bold",
            font = it.font.copy(bold = true)
        ).withoutBack
    })

@ViewModifierDsl3
fun CanAddTheme.textSize(size: Dimension): CanAddTheme = themed(ThemeDerivation {
    it.copy(
        id = "textSize${size.value.toString().filter { it.isLetterOrDigit() }}",
        font = it.font.copy(size = size)
    ).withoutBack
})

@ViewModifierDsl3
val CanAddTheme.italic: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "italic",
            font = it.font.copy(italic = true)
        ).withoutBack
    })

@ViewModifierDsl3
val CanAddTheme.allCaps: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "allCaps",
            font = it.font.copy(allCaps = true)
        ).withoutBack
    })

@ViewModifierDsl3
val CanAddTheme.strikethrough: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "strikethrough",
            font = it.font.copy(strikethrough = true)
        ).withoutBack
    })

@ViewModifierDsl3
val CanAddTheme.underline: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "underline",
            font = it.font.copy(underline = true)
        ).withoutBack
    })

@ViewModifierDsl3
fun CanAddTheme.withSpacing(multiplier: Double): CanAddTheme = themed(ThemeDerivation {
    it.copy(
        id = "withgap${multiplier.toString().replace('.', '_')}",
        gap = it.gap * multiplier
    ).withoutBack
})