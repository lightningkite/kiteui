@file:Suppress("NOTHING_TO_INLINE")

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.signal.CoroutineScopeHelpers
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public abstract class ViewWriter: CoroutineScopeHelpers() {
    public abstract val context: RContext
    public open fun willAddChild(view: RView) {}
    public abstract fun addChild(view: RView)

    public fun split(): ViewWriter {
        val r = object : ViewWriter(), CoroutineScope by this {
            override val context: RContext = this@ViewWriter.context.split()
            override fun addChild(view: RView) = this@ViewWriter.addChild(view)
            override fun willAddChild(view: RView) = this@ViewWriter.willAddChild(view)
        }
        r.beforeNextElementSetup = this@ViewWriter.beforeNextElementSetup
        r._wrapElement = this@ViewWriter._wrapElement
        this@ViewWriter.beforeNextElementSetup = null
        this@ViewWriter._wrapElement = null
        return r
    }

    // Modifier and wrapper handling

    public var beforeNextElementSetup: (RView.() -> Unit)? = null
    public inline fun beforeNextElementSetup(crossinline action: RView.() -> Unit): ViewWrapper {
        val prev = beforeNextElementSetup
        beforeNextElementSetup = { prev?.invoke(this); action() }
        return ViewWrapper
    }

    public var _wrapElement: RView? = null
    public fun wrapNextIn(view: RViewWrapper) {
        val p = _wrapElement ?: this
        p.willAddChild(view)
        _wrapElement = view
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
        view.postSetup()
        p.addChild(view)
    }

    public fun <T : RView> writePre(p: ViewWriter, view: T) {
        p.willAddChild(view)
        _wrapElement = null
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
    }

    public fun <T : RView> writePost(p: ViewWriter, view: T) {
        view.postSetup()
        p.addChild(view)
    }

    @OptIn(ExperimentalContracts::class)
    public inline fun <T : RView> write(view: T, setup: T.() -> Unit): T {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        val p = _wrapElement ?: this
        writePre(p, view)
        setup(view)
        writePost(p, view)
        return view
    }

    @ViewModifierDsl3
    public val Theme.onNext: ViewWrapper get() {
        beforeNextElementSetup {
            themeChoice = ThemeDerivation { this@onNext.withBack }
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    public val Theme.setAsBaseOnNext: ViewWrapper get() {
        beforeNextElementSetup {
            themeChoice = ThemeDerivation { this@setAsBaseOnNext.withoutBack }
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    public val ThemeDerivation.onNext: ViewWrapper get() {
        beforeNextElementSetup {
            val old = themeChoice
            themeChoice = old + this@onNext
        }
        return ViewWrapper
    }

    // Theme, ViewWrapper, ThemeDerivation, Boolean
    // Theme, ViewWrapper, ThemeDerivation, Unit, Boolean, RView
    // contains / minus
    @ViewModifierDsl3
    public operator fun ViewWrapper.minus(view: ViewWrapper): ViewWrapper {
        return if (this !== ViewWrapper.Companion) this else view
    }
    @ViewModifierDsl3
    public operator fun ViewWrapper.minus(view: ViewModifiable): ViewModifiable = this.view() ?: view

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun ViewWrapper.minus(view: Unit): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun ViewWrapper.minus(view: Boolean): ViewWrapper { return ViewWrapper }

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.minus(view: ViewWrapper): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.minus(view: Unit): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.minus(view: Boolean): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.minus(view: ViewModifiable): ViewWrapper { return ViewWrapper }

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun ViewWrapper.contains(view: ViewWrapper): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun ViewWrapper.contains(view: Unit): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun ViewWrapper.contains(view: Boolean): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun ViewWrapper.contains(view: ViewModifiable): Boolean { return true }

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.contains(view: ViewWrapper): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.contains(view: Unit): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.contains(view: Boolean): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3
    public inline operator fun Boolean.contains(view: ViewModifiable): Boolean { return true }
}

public class NewViewWriter(public val calculationContext: CoroutineScope, override val context: RContext) : ViewWriter(), CoroutineScope by calculationContext {
    public var newView: RView? = null
    public override fun addChild(view: RView) {
        newView = view
    }
}
