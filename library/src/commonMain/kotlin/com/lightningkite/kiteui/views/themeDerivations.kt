@file:Suppress("DSL_MARKER_APPLIED_TO_WRONG_TARGET")

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.UnsafeModifier
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.ElementWriter.CanAddTheme
import com.lightningkite.kiteui.views.direct.asBanner
import com.lightningkite.kiteui.views.direct.asNavigation
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
 * This change was made to help encourage safety and keep dynamic theme application
 * consistent with other modifiers in KiteUI.
 * */
@UnsafeModifier
public fun Element.applyDynamicTheme(calculate: ReactiveContext.() -> ThemeDerivation?) {
    var choice: ThemeDerivation = ThemeDerivation.None
    val native = underlyingNativeElement

    @OptIn(ExperimentalKiteUi::class)
    native.themePipeline.add(NativeElementCommonCode.ThemePipeline.Step.dynamicChoice) { choice }

    reactive {
        val newChoice = calculate() ?: ThemeDerivation.None
        if (newChoice != choice) {
            choice = newChoice
            native.refreshTheming()
        }
    }
}

@ViewModifierDsl3
public fun CanAddTheme.themed(theme: ThemeDerivation): CanAddTheme = ThemedWriter(this, theme)

@ViewModifierDsl3
public fun CanAddTheme.dynamicThemed(calculate: ReactiveContext.() -> ThemeDerivation?): CanAddTheme {
    @OptIn(UnsafeModifier::class)
    return beforeSetup { applyDynamicTheme(calculate) }
}

@ViewModifierDsl3
public inline val CanAddTheme.group: CanAddTheme get() = themed(GroupSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.card: CanAddTheme get() = themed(CardSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.fieldTheme: CanAddTheme get() = themed(FieldSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.buttonTheme: CanAddTheme get() = themed(ButtonSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.bar: CanAddTheme get() = themed(BarSemantic).asBanner
@ViewModifierDsl3
public inline val CanAddTheme.nav: CanAddTheme get() = themed(NavSemantic).asNavigation
@ViewModifierDsl3
public inline val CanAddTheme.important: CanAddTheme get() = themed(ImportantSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.critical: CanAddTheme get() = themed(CriticalSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.warning: CanAddTheme get() = themed(WarningSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.danger: CanAddTheme get() = themed(DangerSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.affirmative: CanAddTheme get() = themed(AffirmativeSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.emphasized: CanAddTheme get() = themed(EmphasizedSemantic)
@ViewModifierDsl3
public inline val CanAddTheme.compact: CanAddTheme get() = themed(CompactSemantic)

@ViewModifierDsl3
public val CanAddTheme.bold: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "bold",
            font = it.font.copy(bold = true)
        ).withoutBack
    })

@ViewModifierDsl3
public fun CanAddTheme.textSize(size: Dimension): CanAddTheme = themed(ThemeDerivation {
    it.copy(
        id = "textSize${size.value.toString().filter { it.isLetterOrDigit() }}",
        font = it.font.copy(size = size)
    ).withoutBack
})

@ViewModifierDsl3
public val CanAddTheme.italic: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "italic",
            font = it.font.copy(italic = true)
        ).withoutBack
    })

@ViewModifierDsl3
public val CanAddTheme.allCaps: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "allCaps",
            font = it.font.copy(allCaps = true)
        ).withoutBack
    })

@ViewModifierDsl3
public val CanAddTheme.strikethrough: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "strikethrough",
            font = it.font.copy(strikethrough = true)
        ).withoutBack
    })

@ViewModifierDsl3
public val CanAddTheme.underline: CanAddTheme
    get() = themed(ThemeDerivation {
        it.copy(
            id = "underline",
            font = it.font.copy(underline = true)
        ).withoutBack
    })

@ViewModifierDsl3
public fun CanAddTheme.withSpacing(multiplier: Double): CanAddTheme = themed(ThemeDerivation {
    it.copy(
        id = "withgap${multiplier.toString().replace('.', '_')}",
        gap = it.gap * multiplier
    ).withoutBack
})