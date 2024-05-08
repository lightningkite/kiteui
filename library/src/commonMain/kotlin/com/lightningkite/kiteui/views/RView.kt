package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*

typealias ViewWriter = RView
abstract class RView(val context: NContext) : CalculationContext {
    abstract var opacity: Double
    abstract var exists: Boolean
    abstract var visible: Boolean
    abstract var spacing: Dimension
    abstract var ignoreInteraction: Boolean

    abstract fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    abstract fun requestFocus()

    abstract fun disableAnimation()
    abstract fun enableAnimation()
    inline fun withoutAnimation(action: ()->Unit) {
        disableAnimation()
        try { action() } finally {
            enableAnimation()
        }
    }


    // Theming

    var useBackground: Boolean = false
        set(value) {
            field = value
            theme.state.onSuccess { it?.let { apply(it) } }
        }
    var theme: Theme? = null
    var theme: Theme? = null
    abstract fun applyBackground(theme: Theme?)
    abstract fun applyForeground(theme: Theme)
    fun apply(theme: Theme) {
        applyForeground(theme)
        applyBackground(theme.takeIf { useBackground })
    }


    // Children

    var parent: RView? = null
        private set
    private val internalChildren = ArrayList<RView>()
    val children: List<RView> get() = internalChildren
    fun addChild(index: Int, view: RView) {
        view.parent = this
        internalAddChild(index, view)
        internalChildren.add(index, view)
    }

    fun addChild(view: RView) {
        addChild(children.size, view)
    }

    fun removeChild(index: Int) {
        if (index !in children.indices) throw IllegalArgumentException("$index not in range ${children.indices}")
        internalRemoveChild(index)
        internalChildren.removeAt(index).parent = null
    }

    fun clearChildren() {
        internalClearChildren()
        internalChildren.removeAll {
            it.parent = null
            it.shutdown()
            true
        }
    }

    private fun shutdown() {
        onRemoveSet.invokeAllSafe()
        onRemoveSet.clear()
        for (child in internalChildren)
            child.shutdown()
    }

    abstract fun internalAddChild(index: Int, view: RView)
    abstract fun internalRemoveChild(index: Int)
    abstract fun internalClearChildren()


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
    }

    inline fun <T: RView> write(view: T, setup: T.()->Unit) {
        (_wrapElement ?: this).addChild(view)
        _wrapElement = null
        beforeNextElementSetup?.invoke(view)
        beforeNextElementSetup = null
        setup(view)
        afterNextElementSetup?.invoke(view)
        afterNextElementSetup = null
    }


    // Calculation context

    private val onRemoveSet = ArrayList<() -> Unit>()
    override fun onRemove(action: () -> Unit) {
        onRemoveSet.add(action)
    }

    val working = Property(false)
    private var loadCount = 0
        set(value) {
            field = value
            if (value == 0 && working.value) {
                working.value = false
            } else if (value > 0 && !working.value) {
                working.value = true
            }
        }

    override fun notifyStart() {
        super.notifyStart()
        loadCount++
    }

    override fun notifyLongComplete(result: Result<Unit>) {
        loadCount--
        super.notifyLongComplete(result)
    }


    // Startup

    init {
        theme.state.onSuccess { it?.let {
            apply(it)
            if(useBackground) applyBackground(it)
        } }
        theme.addListener {
            theme.state.onSuccess { it?.let {
                apply(it)
                if(useBackground) applyBackground(it)
            } }
        }
    }
}
