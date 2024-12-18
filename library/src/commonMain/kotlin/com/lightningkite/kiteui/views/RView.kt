package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import kotlinx.coroutines.*
import kotlin.random.Random

abstract class RViewWithAction(context: RContext) : RView(context) {
    private var actionStatusRemove: (() -> Unit)? = null
    var action: Action? = null
        set(value) {
            field = value
            actionSet(value)
        }

    open fun actionSet(value: Action?) {
        actionStatusRemove?.invoke()
        actionStatusRemove = value?.let { listenForWorking(it) }
    }
}

expect abstract class RView constructor(context: RContext) : RViewHelper {
    override var showOnPrint: Boolean
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
abstract class RViewHelper(override val context: RContext) : ViewWriter() {
    var additionalTestingData: Any? = null

    abstract var showOnPrint: Boolean

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
    open var spacing: Dimension? = null
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
                run { applyElevation(if (value.useBackground == UseBackground.Yes) value.theme.elevation else 0.px) }
                run {
                    applyPadding(
                        if (forcePadding ?: (value.useBackground == UseBackground.Yes || hasAlternateBackedStates())) (spacing
                            ?: if (useNavSpacing) value.theme.navSpacing else value.theme.spacing) else null
                    )
                }
                run { applyForeground(value.theme) }
                run { applyBackground(value.theme, value.useBackground != UseBackground.No) }
                if (children.firstOrNull() == viewDebugTarget && viewDebugTarget != null) {
                    println("Parent theme: ${value.theme.id} ${value.theme.foreground}")
                }
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
        .let { if(working.value) it[WorkingSemantic] else it }
        .let { if(loading.value) it[LoadingSemantic] else it }
    open fun hasAlternateBackedStates(): Boolean = false
    fun refreshTheming() {
        if (this == viewDebugTarget) println("refreshTheming")
        if (!fullyStarted) {
            if (this == viewDebugTarget) println("refreshThemeing abandoned due to not fullyStarted")
            return
        }
        if (parent?.fullyStarted == false) {

            if (this == viewDebugTarget) println("refreshThemeing abandoned due to parent $parent not being fully started")
            return
        }
        if (this == viewDebugTarget) println("refreshTheming will set!")
        val t = applyState(themeChoice(parent?.themeAndBack?.theme?.let { it.revert ?: it } ?: Theme.placeholder))
        if (this == viewDebugTarget) println("refreshTheming will set to ${t.theme.id}!")
        themeAndBack = t
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

    private var exceptionHandlers: ExceptionHandlers? = null
    operator fun plusAssign(exceptionHandler: ExceptionHandler) {
        exceptionHandlers?.let {
            it += exceptionHandler
        } ?: run {
            exceptionHandlers = ExceptionHandlers().apply {
                this += exceptionHandler
            }
        }
    }

    private var exceptionToMessages: ExceptionToMessages? = null
    operator fun plusAssign(exceptionToMessage: ExceptionToMessage) {
        exceptionToMessages?.let {
            it += exceptionToMessage
        } ?: run {
            exceptionToMessages = ExceptionToMessages().apply {
                this += exceptionToMessage
            }
        }
    }

    val loading = Property(false)
    private var loadCount = 0
        set(value) {
            field = value
            if (value == 0 && loading.value) {
                loading.value = false
                refreshTheming()
            } else if (value > 0 && !loading.value) {
                loading.value = true
                refreshTheming()
            }
        }
    val working = Property(false)
    private var workCount = 0
        set(value) {
            field = value
            if (value == 0 && working.value) {
                working.value = false
                refreshTheming()
            } else if (value > 0 && !working.value) {
                working.value = true
                refreshTheming()
            }
        }

    private val job = SupervisorJob()
    override val coroutineContext = Dispatchers.Main.immediate + job + CoroutineExceptionHandler { coroutineContext, throwable ->
        if (throwable !is CancellationException) {
            throwable.report(this.toString())
        }
    } + object : StatusListener {
        override fun working(readable: Readable<*>) {
            listenForWorking(readable)
        }

        override fun loading(readable: Readable<*>) {
            listenForStatus(readable)
        }
    }

    fun listenForWorking(readable: Readable<*>): () -> Unit {
        var loading = false
        var excEnder: (() -> Unit)? = null
        val r = readable.addAndRunListener {
            onMainThread {
                val s = readable.state
                if (loading != !s.ready) {
                    if (s.ready) {
                        workCount--
                    } else {
                        workCount++
                    }
                    loading = !s.ready
                }
                excEnder?.invoke()
                s.exception?.let {
                    val myView = this@RViewHelper as RView
                    fun handle(view: RViewHelper): (() -> Unit)? {
                        return view.exceptionHandlers?.handle(myView, true, it) ?: view.parent?.let { handle(it) }
                    }
                    (handle(myView) ?: ExceptionHandlers.root.handle(myView, true, it))?.let { excEnder = it }
                }
            }
        }
        onRemove(r)
        return r
    }

    fun listenForStatus(readable: Readable<*>): () -> Unit {
        var loading = false
        var excEnder: (() -> Unit)? = null
        val r = readable.addAndRunListener {
            onMainThread {
                val s = readable.state
                if (loading != !s.ready) {
                    if (s.ready) {
                        loadCount--
                    } else {
                        loadCount++
                    }
                    loading = !s.ready
                }
                excEnder?.invoke()
                s.exception?.let {
                    val myView = this@RViewHelper as RView
                    fun handle(view: RViewHelper): (() -> Unit)? {
                        return view.exceptionHandlers?.handle(myView, false, it) ?: view.parent?.let { handle(it) }
                    }
                    (handle(myView) ?: ExceptionHandlers.root.handle(myView, false, it))?.let { excEnder = it }
                }
            }
        }
        onRemove(r)
        return r
    }

    fun exceptionToMessage(exception: Exception): ExceptionMessage? {
        val myView = this@RViewHelper as RView
        fun handle(view: RViewHelper): ExceptionMessage? {
            return view.exceptionToMessages?.handle(myView, exception) ?: view.parent?.let { handle(it) }
        }
        return (handle(myView) ?: ExceptionToMessages.root.handle(myView, exception))
    }

    open fun shutdown() {
        job.cancel()
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
    val calculationContext: CoroutineScope get() = this

}

abstract class RViewWrapper(context: RContext) : RView(context) {
    override var spacing: Dimension? = null
        get() = field ?: parent?.spacing
}

