package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeDeriver
import com.lightningkite.kiteui.models.div
import com.lightningkite.kiteui.models.times
import com.lightningkite.kiteui.reactive.CalculationContextStack.end
import com.lightningkite.kiteui.reactive.CalculationContextStack.start
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.viewDebugTarget

abstract class ViewWriter {
    abstract val context: RContext
    open fun willAddChild(view: RView) {}
    abstract fun addChild(view: RView)

    fun split(): ViewWriter = object: ViewWriter() {
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

    fun <T: RView> writePre(p: ViewWriter, view: T) {
//        p.willAddChild(view)
        p.addChild(view)
        _wrapElement = null
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
    }
    fun <T: RView> writePost(p: ViewWriter, view: T) {
        view.postSetup()
        afterNextElementSetup?.invoke(view)
        afterNextElementSetup = null
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

    @Deprecated("Use UseBackground instead", ReplaceWith("UseBackground", "com.lightningkite.kiteui.views.UseBackground"))
    object TransitionNextView {
        @Deprecated("Use UseBackground.Yes instead", ReplaceWith("UseBackground.Yes", "com.lightningkite.kiteui.views.UseBackground")) val Yes = UseBackground.Yes
        @Deprecated("Use UseBackground.No instead", ReplaceWith("UseBackground.No", "com.lightningkite.kiteui.views.UseBackground")) val No = UseBackground.No
    }

    @Deprecated("Use UseBackground on the element itself")
    var transitionNextView: UseBackground
        get() = TODO()
        set(value) {
            afterNextElementSetup { useBackground = value }
        }


    @ViewModifierDsl3
    inline operator fun ViewWrapper.contains(view: RView): Boolean { return true }
    @ViewModifierDsl3
    inline operator fun ViewWrapper.contains(unit: Unit): Boolean { return true }
    @ViewModifierDsl3
    inline operator fun ViewWrapper.contains(boolean: Boolean): Boolean { return true }
    @ViewModifierDsl3
    inline operator fun ViewWrapper.contains(noinline themeDeriver: ThemeDeriver): Boolean {
        beforeNextElementSetup {
            themeChoice = ThemeChoice.Derive(themeDeriver)
            useBackground = UseBackground.Yes
        }
        return true 
    }

    @ViewModifierDsl3
    operator inline fun ViewWrapper.minus(view: RView) = Unit
    @ViewModifierDsl3
    operator inline fun ViewWrapper.minus(unit: Unit) = Unit
    @ViewModifierDsl3
    operator inline fun ViewWrapper.minus(wrapper: ViewWrapper) = ViewWrapper
    @ViewModifierDsl3
    operator inline fun ViewWrapper.minus(noinline themeDeriver: ThemeDeriver): ViewWrapper {
        beforeNextElementSetup {
            themeChoice = ThemeChoice.Derive(themeDeriver)
            useBackground = UseBackground.Yes
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    operator fun Theme.minus(other: RView): ViewWrapper {
        other.themeChoice += ThemeChoice.Set(this)
        other.useBackground = UseBackground.Yes
        return ViewWrapper
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.minus(other: RView): ViewWrapper {
        other.themeChoice += ThemeChoice.Derive(this)
        other.useBackground = UseBackground.Yes
        return ViewWrapper
    }

    @ViewModifierDsl3
    operator fun Theme.contains(other: RView): Boolean {
        other.themeChoice += ThemeChoice.Set(this)
        other.useBackground = UseBackground.Yes
        return true
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.contains(other: RView): Boolean {
        other.themeChoice += ThemeChoice.Derive(this)
        other.useBackground = UseBackground.Yes
        return true
    }

    @ViewModifierDsl3
    operator fun Theme.minus(other: ViewWrapper): ViewWrapper {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Set(this@minus)
            useBackground = UseBackground.Yes
        }
        return ViewWrapper
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.minus(other: ViewWrapper): ViewWrapper {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Derive(this@minus)
            useBackground = UseBackground.Yes
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    operator fun Theme.minus(other: Boolean): ViewWrapper {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Set(this@minus)
            useBackground = UseBackground.Yes
        }
        return ViewWrapper
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.minus(other: Boolean): ViewWrapper {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Derive(this@minus)
            useBackground = UseBackground.Yes
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    operator fun Theme.minus(other: Unit): ViewWrapper {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Set(this@minus)
            useBackground = UseBackground.Yes
        }
        return ViewWrapper
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.minus(other: Unit): ViewWrapper {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Derive(this@minus)
            useBackground = UseBackground.Yes
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    operator fun Theme.contains(other: ViewWrapper): Boolean {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Set(this@contains)
            useBackground = UseBackground.Yes
        }
        return true
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.contains(other: ViewWrapper): Boolean {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Derive(this@contains)
            useBackground = UseBackground.Yes
        }
        return true
    }

    @ViewModifierDsl3
    operator fun Theme.contains(other: Boolean): Boolean {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Set(this@contains)
            useBackground = UseBackground.Yes
        }
        return true
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.contains(other: Boolean): Boolean {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Derive(this@contains)
            useBackground = UseBackground.Yes
        }
        return true
    }

    @ViewModifierDsl3
    operator fun Theme.contains(other: Unit): Boolean {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Set(this@contains)
            useBackground = UseBackground.Yes
        }
        return true
    }
    @ViewModifierDsl3
    operator fun ThemeDeriver.contains(other: Unit): Boolean {
        beforeNextElementSetup {
            themeChoice += ThemeChoice.Derive(this@contains)
            useBackground = UseBackground.Yes
        }
        return true
    }
}

class NewViewWriter(override val context: RContext): ViewWriter() {
    var newView: RView? = null
    override fun addChild(view: RView) {
        newView = view
    }
}

operator fun ViewWrapper.minus(other: ViewWrapper) = ViewWrapper
operator fun ViewWrapper.contains(other: ViewWrapper) : Boolean = true


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
fun ViewWriter.themeFromLast(calculate: (Theme) -> Theme?): ViewWrapper {
    beforeNextElementSetup {
        useBackground = UseBackground.Yes
        themeChoice += ThemeChoice.Derive(calculate)
    }
    return ViewWrapper
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
fun ViewWriter.maybeThemeFromLast(calculate: (Theme) -> Theme?): ViewWrapper {
    beforeNextElementSetup {
        useBackground = UseBackground.IfChanged
        themeChoice += ThemeChoice.Derive(calculate)
    }
    return ViewWrapper
}

@Deprecated("Just bind to themeChoice directly")
@ViewModifierDsl3
fun ViewWriter.tweakTheme(calculate: (Theme) -> Theme?): ViewWrapper {
    beforeNextElementSetup {
        themeChoice += ThemeChoice.Derive(calculate)
    }
    return ViewWrapper
}

@ViewModifierDsl3 val ViewWriter.card: ThemeDeriver get() = Theme::card
@ViewModifierDsl3 val ViewWriter.dialog: ThemeDeriver get() = Theme::dialog
@ViewModifierDsl3 val ViewWriter.mainContent: ThemeDeriver get() = Theme::mainContent
@ViewModifierDsl3 val ViewWriter.fieldTheme: ThemeDeriver get() = Theme::field
@ViewModifierDsl3 val ViewWriter.buttonTheme: ThemeDeriver get() = Theme::button
@ViewModifierDsl3 val ViewWriter.hover: ThemeDeriver get() = Theme::hover
@ViewModifierDsl3 val ViewWriter.down: ThemeDeriver get() = Theme::down
@ViewModifierDsl3 val ViewWriter.selected: ThemeDeriver get() = Theme::selected
@ViewModifierDsl3 val ViewWriter.unselected: ThemeDeriver get() = Theme::unselected
@ViewModifierDsl3 val ViewWriter.disabled: ThemeDeriver get() = Theme::disabled
@ViewModifierDsl3 val ViewWriter.bar: ThemeDeriver get() = Theme::bar
@ViewModifierDsl3 val ViewWriter.nav: ThemeDeriver get() = Theme::nav
@ViewModifierDsl3 val ViewWriter.important: ThemeDeriver get() = Theme::important
@ViewModifierDsl3 val ViewWriter.critical: ThemeDeriver get() = Theme::critical
@ViewModifierDsl3 val ViewWriter.warning: ThemeDeriver get() = Theme::warning
@ViewModifierDsl3 val ViewWriter.danger: ThemeDeriver get() = Theme::danger
@ViewModifierDsl3 val ViewWriter.affirmative: ThemeDeriver get() = Theme::affirmative

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
            title = it.title.copy(bold = true),
            body = it.body.copy(bold = true)
        )
    }

@ViewModifierDsl3
val ViewWriter.italic: ViewWrapper
    get() = tweakTheme {
        it.copy(
            id = "italic",
            title = it.title.copy(italic = true),
            body = it.body.copy(italic = true)
        )
    }

@ViewModifierDsl3
val ViewWriter.allCaps: ViewWrapper
    get() = tweakTheme {
        it.copy(
            id = "allCaps",
            title = it.title.copy(allCaps = true),
            body = it.body.copy(allCaps = true)
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

@ViewModifierDsl3
operator fun ThemeDeriver.minus(other: ThemeDeriver): ThemeDeriver {
    return { other(this(it) ?: it) }
}

fun RView.dynamicTheme(calculate: suspend () -> ThemeDeriver?) {
    val existing = themeChoice
    reactiveScope {
        useBackground = UseBackground.Yes
        themeChoice = existing + calculate()?.let { ThemeChoice.Derive(it) }
    }
}
fun RView.dynamicTweak(calculate: suspend () -> ThemeDeriver?) {
    val existing = themeChoice
    reactiveScope {
        themeChoice = existing + calculate()?.let { ThemeChoice.Derive(it) }
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