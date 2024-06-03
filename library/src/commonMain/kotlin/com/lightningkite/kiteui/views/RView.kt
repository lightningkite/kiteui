package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*

expect abstract class RView : RViewHelper {
    override fun opacitySet(value: Double)
    override fun existsSet(value: Boolean)
    override fun visibleSet(value: Boolean)
    override fun spacingSet(value: Dimension?)
    override fun ignoreInteractionSet(value: Boolean)
    override fun forcePaddingSet(value: Boolean?)
    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)
    override fun requestFocus()
    override fun applyElevation(dimension: Dimension)
    override fun applyPadding(dimension: Dimension?)
    override fun applyBackground(theme: Theme, fullyApply: Boolean)
    override fun applyForeground(theme: Theme)
    override fun internalAddChild(index: Int, view: RView)
    override fun internalRemoveChild(index: Int)
    override fun internalClearChildren()
}

expect inline fun RView.withoutAnimation(action: () -> Unit)
abstract class RViewHelper(override val context: RContext) : CalculationContext, ViewWriter() {
    var opacity: Double = 1.0
        set(value) {
            field = value
            opacitySet(value)
        }

    protected abstract fun opacitySet(value: Double)
    var exists: Boolean = true
        set(value) {
            field = value
            existsSet(value)
        }

    protected abstract fun existsSet(value: Boolean)
    var visible: Boolean = true
        set(value) {
            field = value
            visibleSet(value)
        }

    protected abstract fun visibleSet(value: Boolean)
    var spacing: Dimension? = null
        set(value) {
            field = value
            spacingSet(value)
        }

    protected abstract fun spacingSet(value: Dimension?)
    var ignoreInteraction: Boolean = false
        set(value) {
            field = value
            ignoreInteractionSet(value)
        }

    protected abstract fun ignoreInteractionSet(value: Boolean)
    var forcePadding: Boolean? = null
        set(value) {
            field = value
            forcePaddingSet(value)
        }

    protected abstract fun forcePaddingSet(value: Boolean?)

    var useNavSpacing: Boolean = false

    abstract fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    abstract fun requestFocus()

    companion object {
        var animationsEnabled: Boolean = true
    }


    // Theming

    var useBackground: UseBackground = UseBackground.No
        set(value) {
            field = value
            refreshTheming()
        }
    private var actuallyUseBackground: Boolean = false
    var themeChoice: ThemeChoice? = null
        set(value) {
            field = value
            refreshTheming()
        }
    var theme: Theme = Theme.placeholder
        private set(value) {
            if (value != field) {
                field = value
                applyElevation(if (actuallyUseBackground) theme.elevation else 0.px)
                applyPadding(
                    if (forcePadding ?: (useBackground != UseBackground.No)) (spacing
                        ?: if (useNavSpacing) theme.navSpacing else theme.spacing) else null
                )
                applyForeground(value)
                applyBackground(value, actuallyUseBackground)
                for (child in internalChildren) {
                    if (child.themeChoice !is ThemeChoice.Set)
                        child.refreshTheming()
                }
            }
        }

    protected val parentSpacing: Dimension get() = (parent?.spacing ?: (if(parent?.useNavSpacing == true) parent?.theme?.navSpacing else parent?.theme?.spacing) ?: 0.px)
    protected var fullyStarted = false
    open fun getStateThemeChoice(): ThemeChoice? = null
    protected fun refreshTheming() {
        if (!fullyStarted) return
        if (parent?.fullyStarted == false) return
        val stateThemeChoice = getStateThemeChoice()
        var changed = true
        val futureTheme = when (val t = themeChoice + stateThemeChoice) {
            is ThemeChoice.Derive -> {
                val p = parent?.theme ?: Theme()
                t.derivation(p) ?: run {
                    changed = false
                    p
                }
            }

            is ThemeChoice.Set -> t.theme
            null -> parent?.theme ?: Theme()
        }
        actuallyUseBackground = when (useBackground) {
            UseBackground.No -> false
            UseBackground.Yes -> true
            UseBackground.IfChanged -> changed
        }
        theme = futureTheme
    }

    abstract fun applyElevation(dimension: Dimension)
    abstract fun applyPadding(dimension: Dimension?)
    abstract fun applyBackground(theme: Theme, fullyApply: Boolean)
    abstract fun applyForeground(theme: Theme)


    // Children

    var parent: RView? = null
        set(value) {
            field = value
            if (parent != null) refreshTheming()
        }
    private val internalChildren = ArrayList<RView>()
    val children: List<RView> get() = internalChildren
    fun addChild(index: Int, view: RView) {
        view.parent = this as RView
        internalAddChild(index, view)
        internalChildren.add(index, view)
    }

    override fun addChild(view: RView) {
        view.parent = this as RView
        internalAddChild(children.size, view)
        internalChildren.add(children.size, view)
    }

    fun removeChild(index: Int) {
        if (index !in children.indices) throw IllegalArgumentException("$index not in range ${children.indices}")
        internalRemoveChild(index)
        internalChildren.removeAt(index).also { it.shutdown() }.parent = null
    }

    fun removeChild(view: RView) {
        view.shutdown()
        val i = children.indexOf(view)
        if (i != -1) removeChild(i)
    }

    fun clearChildren() {
        internalClearChildren()
        internalChildren.removeAll {
            it.parent = null
            it.shutdown()
            true
        }
    }

    fun shutdown() {
        onRemoveSet.invokeAllSafe()
        onRemoveSet.clear()
        for (child in internalChildren)
            child.shutdown()
    }

    abstract fun internalAddChild(index: Int, view: RView)
    abstract fun internalRemoveChild(index: Int)
    abstract fun internalClearChildren()
    open fun postSetup() {
        fullyStarted = true
        refreshTheming()
    }


    // Calculation context

    @Deprecated("Not needed anymore", ReplaceWith("this"))
    val calculationContext: CalculationContext get() = this

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
}

