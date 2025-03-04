package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.*
import com.lightningkite.kiteui.reactive.Action
import kotlinx.coroutines.*
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.js.JsName
import kotlin.random.Random

abstract class RViewWithAction(context: RContext) : RView(context) {
    private var actionStatusRemove: (() -> Unit)? = null
    init { onRemove { actionStatusRemove?.invoke(); actionStatusRemove = null } }
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

interface ViewModifiable: CoroutineScope {
    val rView: RView
}

expect abstract class RView constructor(context: RContext) : RViewHelper {
    override var showOnPrint: Boolean
    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)
    override fun requestFocus()
    override fun screenRectangle(): Rect?
    override fun applyTheme(theme: ThemeAndBack)
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
abstract class RViewHelper(override val context: RContext) : ViewWriter(), ViewModifiable {
    override val rView: RView get() = this as RView
    var additionalTestingData: Any? = null

    abstract var showOnPrint: Boolean
    private var isShutdown = false

    open var opacity: Double = 1.0
    open var exists: Boolean = true
    open var visible: Boolean = true
    open var spacing: Dimension? = null
    open var ignoreInteraction: Boolean = false
    var padding: Dimension?
        get() = paddingByEdge?.left
        set(value) { paddingByEdge = value?.let(::Edges) }
    open var paddingByEdge: Edges? = null
    open var transitionId: String? = null

    abstract fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    abstract fun requestFocus()

    companion object {
        var leakDetection: Boolean = false
        var removeBeforeShutdown: Boolean = false
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
                applyTheme(themeAndBack)
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
            ?: (parent?.themeAndBack?.theme?.spacing)
            ?: 0.px)
    protected var fullyStarted = false
    abstract fun applyTheme(theme: ThemeAndBack)
    open fun applyState(theme: ThemeAndBack): ThemeAndBack = theme
        .let { if(working.value) it[WorkingSemantic] else it }
        .let { if(loading.value) it[LoadingSemantic] else it }
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


    // Children

    var parent: RView? = null
        set(value) {
            field = value
            if (parent != null) refreshTheming()
        }
    private val internalChildren = ArrayList<RView>()
    val children: List<RView> get() = internalChildren
    override fun willAddChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call willAddChild was made")
        view.parent = this as RView
    }

    fun addChild(index: Int, view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call addChild was made")
        if (view.parent !== this) view.parent = this as RView
        internalChildren.add(index, view)
        internalAddChild(index, view)
    }

    override fun addChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call addChild was made")
        if (view.parent !== this) view.parent = this as RView
        val index = children.size
        internalChildren.add(index, view)
        internalAddChild(index, view)
    }

    fun removeChild(index: Int) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call removeChild was made")
        if (index !in children.indices) throw IllegalArgumentException("$index not in range ${children.indices}")
        internalRemoveChild(index)
        internalChildren.removeAt(index).also { it.shutdown() }.parent = null
    }

    fun removeChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call removeChild was made")
        view.shutdown()
        val i = children.indexOf(view)
        if (i != -1) removeChild(i)
        else {
            throw IllegalStateException("$view is not a child of $this!")
        }
    }

    fun clearChildren() {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt call to was made ")
        internalClearChildren()
        internalChildren.removeAll {
            it.parent = null
            it.shutdown()
            true
        }
    }


    // Exceptions and Actions

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
    @JsName("contextSetup")
    private fun contextSetup() = MutableCoroutineContext().apply {
        add(Dispatchers.Main.immediate)
        add(this@RViewHelper.job)
        add(CoroutineExceptionHandler { coroutineContext, throwable ->
            if (throwable !is CancellationException) {
                throwable.report(this.toString())
            }
        })
        add(object : StatusListener {
            override fun working(readable: Readable<*>) {
                listenForWorking(readable)
            }

            override fun loading(readable: Readable<*>) {
                listenForStatus(readable)
            }
        })
    }
    override val coroutineContext: CoroutineContext = contextSetup()

    internal fun listenForWorking(readable: Readable<*>): () -> Unit {
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

    internal fun listenForStatus(readable: Readable<*>): () -> Unit {
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


    // Cleanup Insurance
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
        isShutdown = true
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

class MutableCoroutineContext: CoroutineContext {
    val list = ArrayList<CoroutineContext.Element>()
    var interceptor: CoroutineContext.Element? = null
    fun add(context: CoroutineContext) {
        context.fold(Unit) { _, element -> add(element) }
    }
    fun add(element: CoroutineContext.Element) {
        if(element.key == ContinuationInterceptor.Key) interceptor = element
        else list.add(element)
    }
    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
        var out: R = interceptor?.let { operation(initial, it) } ?: initial
        out = list.fold(out, operation)
        return out
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        if(key == ContinuationInterceptor.Key) return interceptor as E?
        return list.lastOrNull { it.key == key } as E?
    }

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        return MutableCoroutineContext().apply {
            if(key != ContinuationInterceptor.Key) {
                this@MutableCoroutineContext.interceptor?.let { this@apply.interceptor = it }
                this@MutableCoroutineContext.list.forEach { if(it.key != key) this@apply.add(it) }
            } else {
                this@apply.list.addAll(this@MutableCoroutineContext.list)
            }
        }
    }
}