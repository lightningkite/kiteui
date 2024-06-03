package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.reactive.CalculationContextStack.end
import com.lightningkite.kiteui.reactive.CalculationContextStack.start

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
}

class NewViewWriter(override val context: RContext): ViewWriter() {
    var newView: RView? = null
    override fun addChild(view: RView) {
        newView = view
    }
}