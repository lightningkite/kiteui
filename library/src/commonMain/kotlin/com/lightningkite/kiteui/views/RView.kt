package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.*
import com.lightningkite.kiteui.reactive.Action
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.js.JsName
import kotlin.random.Random

public abstract class RViewWithAction(context: RContext) : RView(context) {
    private var actionStatusRemove: (() -> Unit)? = null
    init { onRemove { actionStatusRemove?.invoke(); actionStatusRemove = null } }
    public var action: Action? = null
        set(value) {
            field = value
            actionSet(value)
        }

    public open fun actionSet(value: Action?) {
        actionStatusRemove?.invoke()
        actionStatusRemove = value?.let { listenForWorking(it) }
    }
}

public interface ViewModifiable: CoroutineScope {
    public val rView: RView
}

public expect abstract class RView constructor(context: RContext) : RViewHelper {
    override var showOnPrint: Boolean
    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)
    override fun requestFocus()
    override fun screenRectangle(): Rect?
    override fun applyTheme(theme: ThemeAndBack)
    override fun internalAddChild(index: Int, view: RView)
    override fun internalRemoveChild(index: Int)
    override fun internalClearChildren()
}

public fun RView.rectangleRelativeTo(other: RView): Rect? {
    val myRect = screenRectangle() ?: return null
    val otherRect = other.screenRectangle() ?: return null
    return Rect(
        left = myRect.left - otherRect.left,
        top = myRect.top - otherRect.top,
        right = myRect.right - otherRect.left,
        bottom = myRect.bottom - otherRect.top,
    )
}

public expect val RView.areAnimationsEnabled: Boolean
public expect inline fun RView.withoutAnimation(action: () -> Unit)
public abstract class RViewHelper(override val context: RContext) : ViewWriter(), ViewModifiable {
    public override val rView: RView get() = this as RView
    public var additionalTestingData: Any? = null

    public open val cannotBeCovered: Boolean get() = true

    public abstract var showOnPrint: Boolean
    public var isShutdown: Boolean = false
        private set

    public open var opacity: Double = 1.0
    public open var shown: Boolean = true
    @Deprecated("Renamed to 'shown'", ReplaceWith("shown"))
    public var exists: Boolean
        get() = shown
        set(value) { shown = value }
    public open var visible: Boolean = true
    public open var gap: Dimension? = null
    @Deprecated("Renamed to 'gap'", ReplaceWith("gap"))
    public var spacing: Dimension?
        get() = gap
        set(value) { gap = value }
    public open var ignoreInteraction: Boolean = false
    public var padding: Dimension?
        get() = paddingByEdge?.left
        set(value) { paddingByEdge = value?.let(::Edges) }
    public open var paddingByEdge: Edges? = null
    public open var transitionId: String? = null

    // drag 'n drop
    public open var dragData: DragData? = null
    public open var dropTargetDelegate: DropTargetDelegate? = null

    public abstract fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    public abstract fun requestFocus()

    public companion object {
        public var leakDetection: Boolean = false
        public var removeBeforeShutdown: Boolean = false
    }


    // Theming

    private val id = Random.nextInt()
    public var themeTakeNonCascadingFromParent: Boolean = false
        set(value) {
            field = value
            refreshTheming()
        }
    public var themeChoice: ThemeDerivation = ThemeDerivation.none
        set(value) {
            field = value
            refreshTheming()
        }
    public val theme: Theme get() = themeAndBack.theme
    public var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
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

    public open val mySpacingForChildren: Dimension
        get()  {
            val pad = padding ?: themeAndBack.theme.padding.top
            val gap = gap ?: themeAndBack.theme.gap
            return minOf(pad, gap)
        }
    protected var fullyStarted: Boolean = false
    public abstract fun applyTheme(theme: ThemeAndBack)
    public open fun applyState(theme: ThemeAndBack): ThemeAndBack = theme
        .let { if(working.value) it[WorkingSemantic] else it }
        .let { if(loading.value) it[LoadingSemantic] else it }
    public fun refreshTheming() {
        if (this == viewDebugTarget) println("refreshTheming")
        if (!fullyStarted) {
            if (this == viewDebugTarget) println("refreshThemeing abandoned due to not fullyStarted")
            return
        }
        if (parent?.fullyStarted == false) {

            if (this == viewDebugTarget) println("refreshThemeing abandoned due to parent $parent not being fully started")
            return
        }
        val themeBorrowed = if(themeTakeNonCascadingFromParent) parent?.theme ?: Theme.placeholder
        else parent?.theme?.let { it.revert ?: it } ?: Theme.placeholder
        if (this == viewDebugTarget) println("refreshTheming will set!  Parent theme is ${themeBorrowed.id}")
        val t = applyState(themeChoice(themeBorrowed))
        if (this == viewDebugTarget) println("refreshTheming will set to ${t.theme.id}!")
        themeAndBack = t
    }


    // Children

    public var parent: RView? = null
        set(value) {
            field = value
            if (parent != null) refreshTheming()
        }
    private val internalChildren = ArrayList<RView>()
    public val children: List<RView> get() = internalChildren
    public override fun willAddChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call willAddChild was made")
        view.parent = this as RView
    }

    public fun addChild(index: Int, view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call addChild was made")
        if (view.parent !== this) view.parent = this as RView
        internalChildren.add(index, view)
        internalAddChild(index, view)
    }

    public override fun addChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call addChild was made")
        if (view.parent !== this) view.parent = this as RView
        val index = children.size
        internalChildren.add(index, view)
        internalAddChild(index, view)
    }

    public fun removeChild(index: Int) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call removeChild was made")
        if (index !in children.indices) throw IllegalArgumentException("$index not in range ${children.indices}")
        internalRemoveChild(index)
        internalChildren.removeAt(index).also { it.shutdown() }.parent = null
    }

    public fun removeChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call removeChild was made")
        view.shutdown()
        val i = children.indexOf(view)
        if (i != -1) removeChild(i)
        else {
            throw IllegalStateException("$view is not a child of $this!")
        }
    }

    public fun clearChildren() {
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
    public operator fun plusAssign(exceptionHandler: ExceptionHandler) {
        exceptionHandlers?.let {
            it += exceptionHandler
        } ?: run {
            exceptionHandlers = ExceptionHandlers().apply {
                this += exceptionHandler
            }
        }
    }

    private var exceptionToMessages: ExceptionToMessages? = null
    public operator fun plusAssign(exceptionToMessage: ExceptionToMessage) {
        exceptionToMessages?.let {
            it += exceptionToMessage
        } ?: run {
            exceptionToMessages = ExceptionToMessages().apply {
                this += exceptionToMessage
            }
        }
    }

    public val loading: Property<Boolean> = Property(false)
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
    public val working: Property<Boolean> = Property(false)
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
        add(Dispatchers.Main.immediate)
    }
    public override val coroutineContext: CoroutineContext = contextSetup()

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

    public fun exceptionToMessage(exception: Exception): ExceptionMessage? {
        val myView = this@RViewHelper as RView
        fun handle(view: RViewHelper): ExceptionMessage? {
            return view.exceptionToMessages?.handle(myView, exception) ?: view.parent?.let { handle(it) }
        }
        return (handle(myView) ?: ExceptionToMessages.root.handle(myView, exception))
    }

    public val shutdownListeners: MutableSet<() -> Unit> = mutableSetOf<() -> Unit>()
    public fun onShutdown(action: () -> Unit): () -> Unit {
        shutdownListeners.add(action)
        return { shutdownListeners.remove(action) }
    }

    // Cleanup Insurance
    public open fun shutdown() {
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
        shutdownListeners.forEach { it() }
    }

    public open fun leakDetect() {
        WeakReference(this).checkLeakAfterDelay(1000)
    }

    public abstract fun internalAddChild(index: Int, view: RView)
    public abstract fun internalRemoveChild(index: Int)
    public abstract fun internalClearChildren()
    public open fun postSetup() {
        fullyStarted = true
        refreshTheming()
    }

    public abstract fun screenRectangle(): Rect?


    // Calculation context

    @Deprecated("Not needed anymore", ReplaceWith("this"))
    public val calculationContext: CoroutineScope get() = this

    public var debugName: String? = null
    public override fun toString(): String {
        return debugName ?: (theme.id + " " + this::class.toString().removePrefix("class ") + "@" + this.identityHashCode().toString(16))
    }
}

public interface DropTargetDelegate {
    public fun over(event: DragEvent): Boolean = true
    public fun drop(event: DragEvent): Boolean
}

public abstract class RViewWrapper(context: RContext) : RView(context) {
    public override var gap: Dimension? = null
        get() = field ?: parent?.gap
}

public class MutableCoroutineContext: CoroutineContext {
    public val list: ArrayList<CoroutineContext.Element> = ArrayList<CoroutineContext.Element>()
    public fun add(context: CoroutineContext) {
        context.fold(Unit) { _, element -> add(element) }
    }
    public fun add(element: CoroutineContext.Element) {
        list.add(element)
    }
    override fun <R> fold(initial: R, operation: (R, CoroutineContext.Element) -> R): R {
        return list.fold(initial, operation)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? {
        for(index in list.lastIndex downTo 0) {
            return list[index][key] ?: continue
        }
        return null
    }

    public override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext {
        return MutableCoroutineContext().apply {
            this@MutableCoroutineContext.list.forEach { if(it[key] == null) this@apply.add(it) }
        }
    }
}