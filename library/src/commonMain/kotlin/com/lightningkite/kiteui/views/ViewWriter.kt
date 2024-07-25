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

    var _wrapElement: RView? = null
    fun wrapNextIn(view: RView) {
        (_wrapElement ?: this).addChild(view)
        _wrapElement = view
        view.postSetup()
    }

    fun <T : RView> writePre(p: ViewWriter, view: T) {
        start(view)
        p.willAddChild(view)
        _wrapElement = null
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
    }

    fun <T : RView> writePost(p: ViewWriter, view: T) {
        view.postSetup()
        p.addChild(view)
    }

    inline fun <T : RView> write(view: T, setup: T.() -> Unit): T {
        val p = _wrapElement ?: this
        writePre(p, view)
        try {
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