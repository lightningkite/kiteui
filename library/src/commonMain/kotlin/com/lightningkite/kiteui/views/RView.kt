package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

expect abstract class RView : RViewHelper {
    override fun opacitySet(value: Double)
    override fun existsSet(value: Boolean)
    override fun visibleSet(value: Boolean)
    override fun spacingSet(value: Dimension?)
    override fun ignoreInteractionSet(value: Boolean)
    override fun forcePaddingSet(value: Boolean?)
    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)
    override fun requestFocus()
    override fun screenRectangle(): Rect?
    override fun applyElevation(dimension: Dimension)
    override fun applyPadding(dimension: Dimension?)
    override fun applyBackground(theme: Theme, fullyApply: Boolean)
    override fun applyForeground(theme: Theme)
    override fun internalAddChild(index: Int, view: RView)
    override fun internalRemoveChild(index: Int)
    override fun internalClearChildren()
}

fun RView.rectangleRelativeTo(other: RView): Rect? {
    val myRect = screenRectangle() ?: return null
    val otherRect = other.screenRectangle() ?: return null
    return Rect(
        left = myRect.left - otherRect.left,
        top = myRect.top - otherRect.top,
        right = myRect.right - otherRect.left,
        bottom = myRect.bottom - otherRect.top,
    )
}

expect inline fun RView.withoutAnimation(action: () -> Unit)
abstract class RViewHelper(override val context: RContext) : CalculationContext, ViewWriter() {
    var additionalTestingData: Any? = null

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
    var transitionId: String? = null
        set(value) {
            field = value
            transitionIdSet(value)
        }

    protected open fun transitionIdSet(value: String?) {}

    var useNavSpacing: Boolean = false

    abstract fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    abstract fun requestFocus()

    companion object {
        var animationsEnabled: Boolean = true
        var leakDetection: Boolean = false
        var removeBeforeShutdown: Boolean = false
        val leakLog = ConsoleRoot.tag("RViewLeaks")
    }


    // Theming

    private val id = Random.nextInt()
    var themeChoice: ThemeDerivation = ThemeDerivation.none
        set(value) {
            field = value
            refreshTheming()
        }
    val theme: Theme get() = themeAndBack.theme
    var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
        private set(value) {
            if (value != field) {
                field = value
                run { applyElevation(if (value.useBackground) value.theme.elevation else 0.px) }
                run {
                    applyPadding(
                        if (forcePadding ?: (value.useBackground || hasAlternateBackedStates())) (spacing
                            ?: if (useNavSpacing) value.theme.navSpacing else value.theme.spacing) else null
                    )
                }
                run { applyForeground(value.theme) }
                run { applyBackground(value.theme, value.useBackground) }
                for (child in internalChildren) {
//                    if (child.themeChoice !is ThemeChoice.Set)
                    child.refreshTheming()
                }
            }
        }

    protected val parentSpacing: Dimension
        get() = (parent?.spacing
            ?: (if (parent?.useNavSpacing == true) parent?.themeAndBack?.theme?.navSpacing else parent?.themeAndBack?.theme?.spacing)
            ?: 0.px)
    protected var fullyStarted = false
    open fun applyState(theme: ThemeAndBack): ThemeAndBack = theme
    open fun hasAlternateBackedStates(): Boolean = false
    fun refreshTheming() {
        if (!fullyStarted) return
        if (parent?.fullyStarted == false) return
        themeAndBack =
            applyState(themeChoice(parent?.themeAndBack?.theme?.let { it.revert ?: it } ?: Theme.placeholder))
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
    override fun willAddChild(view: RView) {
        view.parent = this as RView
    }

    fun addChild(index: Int, view: RView) {
        if (view.parent !== this) view.parent = this as RView
        internalChildren.add(index, view)
        internalAddChild(index, view)
    }

    override fun addChild(view: RView) {
        if (view.parent !== this) view.parent = this as RView
        val index = children.size
        internalChildren.add(index, view)
        internalAddChild(index, view)
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
        else {
            throw IllegalStateException("$view is not a child of $this!")
        }
    }

    fun clearChildren() {
        internalClearChildren()
        internalChildren.removeAll {
            it.parent = null
            it.shutdown()
            true
        }
    }

    override val coroutineContext: Job = Job()
    open fun shutdown() {
        coroutineContext.cancel()
        if (removeBeforeShutdown) {
            for (index in internalChildren.lastIndex downTo 0) {
                internalRemoveChild(index)
                internalChildren.removeAt(index).shutdown()
            }
        } else {
            internalChildren.forEach { it.shutdown() }
            internalChildren.clear()
        }
        if (leakDetection) leakDetect()
    }

    open fun leakDetect() {
        WeakReference(this).checkLeakAfterDelay(1000)
    }

    abstract fun internalAddChild(index: Int, view: RView)
    abstract fun internalRemoveChild(index: Int)
    abstract fun internalClearChildren()
    open fun postSetup() {
        fullyStarted = true
        refreshTheming()
    }

    abstract fun screenRectangle(): Rect?


    // Calculation context

    @Deprecated("Not needed anymore", ReplaceWith("this"))
    val calculationContext: CalculationContext get() = this

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

