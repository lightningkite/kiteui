package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.UnsafeModifier
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.ElementWriter.CanAddTheme
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.reactive

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

@UnsafeModifier
/**
 * Apply a dynamic theme directly to an [Element]
 *
 * The preferred version of this operation is [dynamicThemed] (with a 'd') applied as a modifier __outside__ the element:
 *
 * ```kotlin
 * val myTheme: Reactive<ThemeDerivation> = ...
 *
 * // from this
 * frame {
 *    applyDynamicTheme { myTheme() }
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
 * static at the time they are defined. Also, you can only call `applyDynamicTheme` once per element.
 *
 * If you change the `themeChoice` after a call to `applyDynamicTheme` you won't get the result you expect. Similarly,
 * if you call `applyDynamicTheme` twice on an element you'll get weird bugs. The modifier syntax enforces this contract.
 * If you apply a dynamic theme directly _you_ are responsible to uphold this contract.
 *
 * ```kotlin
 * frame {
 *    themeChoice += CardSemantic
 *    applyDynamicTheme { myTheme() } // dynamic theme applied
 *
 *    themeChoice += ImportantSemantic // <- Bug!!
 *    applyDynamicTheme { myTheme2() } // <- Bug!!
 * }
 * ```
 * */
fun Element.applyDynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?) {
    val e = underlyingNativeElement
    @OptIn(ExperimentalKiteUi::class)
    reactive {
        e.themePipeline.set(
            NativeElementCommonCode.ThemePipeline.Step.dynamicChoice,
            calculate()
        )
        e.refreshTheming()
    }
}

@ViewModifierDsl3
fun CanAddTheme.themed(theme: ThemeDerivation): CanAddTheme = ThemedWriter(this, theme)

@ViewModifierDsl3
fun CanAddTheme.dynamicThemed(calculate: ReactiveContext.() -> ThemeDerivation?): ElementWriter.CanAddScrolling {
    @OptIn(UnsafeModifier::class)
    return beforeSetup { applyDynamicTheme(calculate) }
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