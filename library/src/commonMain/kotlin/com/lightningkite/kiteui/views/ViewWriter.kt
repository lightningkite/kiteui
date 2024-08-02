package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.CalculationContextStack.end
import com.lightningkite.kiteui.reactive.CalculationContextStack.start
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.viewDebugTarget

abstract class ViewWriter {
    abstract val context: RContext
    open fun willAddChild(view: RView) {}
    abstract fun addChild(view: RView)

    fun split(): ViewWriter = object : ViewWriter() {
        override val context: RContext = this@ViewWriter.context.split()
        override fun addChild(view: RView) {
            this@ViewWriter.addChild(view)
        }
    }

    // Modifier and wrapper handling

    var beforeNextElementSetup: (RView.() -> Unit)? = null
    inline fun beforeNextElementSetup(crossinline action: RView.() -> Unit) {
        val prev = beforeNextElementSetup
        beforeNextElementSetup = { prev?.invoke(this); action() }
    }

    var afterNextElementSetup: (RView.() -> Unit)? = null
    inline fun afterNextElementSetup(crossinline action: RView.() -> Unit) {
        val prev = afterNextElementSetup
        afterNextElementSetup = { prev?.invoke(this); action() }
    }

    var _wrapElement: RView? = null
    fun wrapNextIn(view: RView) {
        (_wrapElement ?: this).addChild(view)
        _wrapElement = view
        view.postSetup()
    }

    fun <T : RView> writePre(p: ViewWriter, view: T) {
        p.willAddChild(view)
        _wrapElement = null
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
    }

    fun <T : RView> writePost(p: ViewWriter, view: T) {
        view.postSetup()
        afterNextElementSetup?.invoke(view)
        afterNextElementSetup = null
        p.addChild(view)
    }

    inline fun <T : RView> write(view: T, setup: T.() -> Unit): T {
        val p = _wrapElement ?: this
        start(view)
        try {
            writePre(p, view)
            setup(view)
            writePost(p, view)
        } finally {
            end(view)
        }
        return view
    }

    @Deprecated(
        "Use UseBackground instead",
        ReplaceWith("UseBackground", "com.lightningkite.kiteui.views.UseBackground")
    )
    object TransitionNextView {
        @Deprecated(
            "Use UseBackground.Yes instead",
            ReplaceWith("UseBackground.Yes", "com.lightningkite.kiteui.views.UseBackground")
        )
        val Yes = Unit
        @Deprecated(
            "Use UseBackground.No instead",
            ReplaceWith("UseBackground.No", "com.lightningkite.kiteui.views.UseBackground")
        )
        val No = Unit
    }

    @Deprecated("Use UseBackground on the element itself")
    var transitionNextView: Unit
        get() = Unit
        set(value) {
//            afterNextElementSetup { useBackground = value }
        }

    @ViewModifierDsl3
    val Theme.onNext: ViewWrapper get() {
        beforeNextElementSetup {
            themeChoice = ThemeDerivation { this@onNext.withBack }
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    val ThemeDerivation.onNext: ViewWrapper get() {
        beforeNextElementSetup {
            val old = themeChoice
            themeChoice = old + this@onNext
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    val ThemeDerivation.onOnlyNext: ViewWrapper get() {
        lateinit var old: ThemeDerivation
        beforeNextElementSetup {
            old = themeChoice
            themeChoice = old + this@onOnlyNext
        }
        afterNextElementSetup {
            themeChoice = old
        }
        return ViewWrapper
    }

    // Theme, ViewWrapper, ThemeDerivation, Boolean
    // Theme, ViewWrapper, ThemeDerivation, Unit, Boolean, RView
    // contains / minus
    @ViewModifierDsl3 inline operator fun ViewWrapper.minus(view: ViewWrapper): ViewWrapper { return ViewWrapper }
    @ViewModifierDsl3 inline operator fun ViewWrapper.minus(view: Unit): ViewWrapper { return ViewWrapper }
    @ViewModifierDsl3 inline operator fun ViewWrapper.minus(view: Boolean): ViewWrapper { return ViewWrapper }
    @ViewModifierDsl3 inline operator fun ViewWrapper.minus(view: ViewWriter): ViewWrapper { return ViewWrapper }

    @ViewModifierDsl3 inline operator fun Boolean.minus(view: ViewWrapper): ViewWrapper { return ViewWrapper }
    @ViewModifierDsl3 inline operator fun Boolean.minus(view: Unit): ViewWrapper { return ViewWrapper }
    @ViewModifierDsl3 inline operator fun Boolean.minus(view: Boolean): ViewWrapper { return ViewWrapper }
    @ViewModifierDsl3 inline operator fun Boolean.minus(view: ViewWriter): ViewWrapper { return ViewWrapper }

    @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: ViewWrapper): Boolean { return true }
    @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: Unit): Boolean { return true }
    @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: Boolean): Boolean { return true }
    @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: ViewWriter): Boolean { return true }

    @ViewModifierDsl3 inline operator fun Boolean.contains(view: ViewWrapper): Boolean { return true }
    @ViewModifierDsl3 inline operator fun Boolean.contains(view: Unit): Boolean { return true }
    @ViewModifierDsl3 inline operator fun Boolean.contains(view: Boolean): Boolean { return true }
    @ViewModifierDsl3 inline operator fun Boolean.contains(view: ViewWriter): Boolean { return true }
}

class NewViewWriter(override val context: RContext) : ViewWriter() {
    var newView: RView? = null
    override fun addChild(view: RView) {
        newView = view
    }
}

operator fun ViewWrapper.minus(other: ViewWrapper) = ViewWrapper
operator fun ViewWrapper.contains(other: ViewWrapper): Boolean = true


@ViewModifierDsl3
val ViewWriter.debugNext: ViewWrapper
    get() {
        beforeNextElementSetup {
            viewDebugTarget = this
        }
        afterNextElementSetup {
            if (viewDebugTarget == this) viewDebugTarget = null
        }
        return ViewWrapper
    }

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
val ViewWriter.card: ViewWrapper get() = CardSemantic.onNext
@ViewModifierDsl3
val ViewWriter.dialog: ViewWrapper get() = DialogSemantic.onNext
@ViewModifierDsl3
val ViewWriter.mainContent: ViewWrapper get() = MainContentSemantic.onNext
@ViewModifierDsl3
val ViewWriter.fieldTheme: ViewWrapper get() = FieldSemantic.onNext
@ViewModifierDsl3
val ViewWriter.buttonTheme: ViewWrapper get() = ButtonSemantic.onNext
@ViewModifierDsl3
val ViewWriter.hover: ViewWrapper get() = HoverSemantic.onNext
@ViewModifierDsl3
val ViewWriter.down: ViewWrapper get() = DownSemantic.onNext
@ViewModifierDsl3
val ViewWriter.selected: ViewWrapper get() = SelectedSemantic.onNext
@ViewModifierDsl3
val ViewWriter.unselected: ViewWrapper get() = UnselectedSemantic.onNext
@ViewModifierDsl3
val ViewWriter.disabled: ViewWrapper get() = DisabledSemantic.onNext
@ViewModifierDsl3
val ViewWriter.bar: ViewWrapper get() = BarSemantic.onNext
@ViewModifierDsl3
val ViewWriter.nav: ViewWrapper get() = NavSemantic.onNext
@ViewModifierDsl3
val ViewWriter.important: ViewWrapper get() = ImportantSemantic.onNext
@ViewModifierDsl3
val ViewWriter.critical: ViewWrapper get() = CriticalSemantic.onNext
@ViewModifierDsl3
val ViewWriter.warning: ViewWrapper get() = WarningSemantic.onNext
@ViewModifierDsl3
val ViewWriter.danger: ViewWrapper get() = DangerSemantic.onNext
@ViewModifierDsl3
val ViewWriter.affirmative: ViewWrapper get() = AffirmativeSemantic.onNext

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


fun RView.dynamicTheme(calculate: suspend () -> ThemeDerivation?) {
    val existing = themeChoice
    reactiveScope {
        themeChoice = existing + (calculate() ?: ThemeDerivation.none)
    }
}

//var RView.themeDeriver: ThemeDeriver?
//    get() = (themeChoice as? ThemeChoice.Derive)?.derivation
//    set(value) {
//        themeChoice = value?.let { ThemeChoice.Derive(it) }
//        useBackground = UseBackground.IfChanged
//    }
//var RView.themeTweak: ThemeDeriver?
//    get() = (themeChoice as? ThemeChoice.Derive)?.derivation
//    set(value) {
//        themeChoice = value?.let { ThemeChoice.Derive(it) }
//    }