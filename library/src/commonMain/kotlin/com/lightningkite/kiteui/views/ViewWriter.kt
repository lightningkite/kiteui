package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.CalculationContextStack

abstract class ViewWriter {
    abstract val context: RContext
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

    inline fun <T : RView> write(view: T, setup: T.() -> Unit): T {
        (_wrapElement ?: this).addChild(view)
        _wrapElement = null
        CalculationContextStack.useIn(view) {
            beforeNextElementSetup?.invoke(view)
            beforeNextElementSetup = null
            setup(view)
            view.postSetup()
            afterNextElementSetup?.invoke(view)
            afterNextElementSetup = null
        }
        return view
    }
}