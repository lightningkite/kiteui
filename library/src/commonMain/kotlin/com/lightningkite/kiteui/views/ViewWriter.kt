@file:Suppress("NOTHING_TO_INLINE")

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.readable.CoroutineScopeStack
import com.lightningkite.readable.CoroutineScopeStack.end
import com.lightningkite.readable.CoroutineScopeStack.start
import kotlinx.coroutines.CoroutineScope
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

abstract class ViewWriter: CoroutineScope {
    abstract val context: RContext
    open fun willAddChild(view: RView) {}
    abstract fun addChild(view: RView)
    var lastWrittenView: RView? = null

    fun split(): ViewWriter = object : ViewWriter(), CoroutineScope by this {
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
    fun wrapNextIn(view: RViewWrapper) {
        val p = _wrapElement ?: this
        p.willAddChild(view)
        _wrapElement = view
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
        view.postSetup()
        p.addChild(view)
    }

    fun <T : RView> end(view: T) = CoroutineScopeStack.end(view)
    fun <T : RView> writePre(view: T) {
        val p = _wrapElement ?: this
        p.willAddChild(view)
        start(view)
        _wrapElement = null
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
    }

    fun <T : RView> writePost(view: T) {
        val p = _wrapElement ?: this
        view.postSetup()
        p.addChild(view)
        lastWrittenView = view
    }

    fun endFail(view: RView, e: Exception) {
        ConsoleRoot.warn("Failed to setup $view: $e")
        e.printStackTrace2()
        throw e
    }

    @OptIn(ExperimentalContracts::class)
    inline fun <T : RView> write(view: T, setup: T.() -> Unit): T {
        contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
        writePre(view)
        try {
            setup(view)
            writePost(view)
        } catch(e: Exception) {
            endFail(view, e)
        } finally {
            end(view)
        }
        return view
    }

    @ViewModifierDsl3
    val Theme.onNext: ViewWrapper get() {
        beforeNextElementSetup {
            themeChoice = ThemeDerivation { this@onNext.withBack }
        }
        return ViewWrapper
    }

    @ViewModifierDsl3
    val Theme.setAsBaseOnNext: ViewWrapper get() {
        beforeNextElementSetup {
            themeChoice = ThemeDerivation { this@setAsBaseOnNext.withoutBack }
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
    @ViewModifierDsl3 inline operator fun <T: ViewModifiable> ViewWrapper.minus(view: T): T = view

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun ViewWrapper.minus(view: Unit): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun ViewWrapper.minus(view: Boolean): ViewWrapper { return ViewWrapper }

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.minus(view: ViewWrapper): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.minus(view: Unit): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.minus(view: Boolean): ViewWrapper { return ViewWrapper }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.minus(view: ViewModifiable): ViewWrapper { return ViewWrapper }

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: ViewWrapper): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: Unit): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: Boolean): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun ViewWrapper.contains(view: ViewModifiable): Boolean { return true }

    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.contains(view: ViewWrapper): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.contains(view: Unit): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.contains(view: Boolean): Boolean { return true }
    @Deprecated("Will not be supported in the future.  Please use proper modifier style with -.") @ViewModifierDsl3 inline operator fun Boolean.contains(view: ViewModifiable): Boolean { return true }
}

class NewViewWriter(val calculationContext: CoroutineScope, override val context: RContext) : ViewWriter(), CoroutineScope by calculationContext {
    var newView: RView? = null
    override fun addChild(view: RView) {
        newView = view
    }
}
