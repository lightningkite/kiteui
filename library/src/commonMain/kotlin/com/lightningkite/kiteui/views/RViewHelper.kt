package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.exceptions.ExceptionHandlers
import com.lightningkite.kiteui.exceptions.ExceptionMessage
import com.lightningkite.kiteui.exceptions.ExceptionToMessage
import com.lightningkite.kiteui.exceptions.ExceptionToMessages
import com.lightningkite.kiteui.identityHashCode
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.LoadingSemantic
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.WorkingSemantic
import com.lightningkite.kiteui.onMainThread
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.coroutines.CoroutineContext
import kotlin.js.JsName
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

public abstract class RViewHelper(override val context: RContext) : ViewWriter(), ViewModifiable {
    public override val rView: RView get() = this as RView

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
    public var additionalPadding: Edges? = null
        set(value) {
            field = value
            refreshPadding()
        }
    public open var paddingByEdge: Edges? = null
        set(value) {
            field = value
            refreshPadding()
        }
    public open var transitionId: String? = null

    // Safe insets handling
    public var lastSetWeight: Float? = null
    public var lastSetHorizontalAlign: Align = Align.Stretch
    public var lastSetVerticalAlign: Align = Align.Stretch

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

    private val id = Random.Default.nextInt()
    public var themeTakeNonCascadingFromParent: Boolean = false
        set(value) {
            field = value
            refreshTheming()
        }
    public var themeChoice: ThemeDerivation = ThemeDerivation.Companion.none
        set(value) {
            field = value
            refreshTheming()
        }
    public val theme: Theme get() = themeAndBack.theme
    public var themeAndBack: ThemeAndBack = Theme.Companion.placeholder.withBack
        private set(value) {
            if (value != field) {
                field = value
                applyTheme(value)
                refreshPadding()
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
    public val appliedPadding: Edges
        get() = (paddingByEdge ?: if(themeAndBack.padding) theme.padding else Edges.ZERO).let {
        if(additionalPadding != null) it + additionalPadding
        else it
    }
    protected var fullyStarted: Boolean = false
    public abstract fun applyTheme(theme: ThemeAndBack)
    public open fun applyState(theme: ThemeAndBack): ThemeAndBack = theme
        .let { if(working.value) it[WorkingSemantic] else it }
        .let { if(loading.value) it[LoadingSemantic] else it }
    public open fun refreshPadding() {
    }
    public fun refreshTheming() {
        if (this == viewDebugTarget) println("refreshTheming")
        if (!fullyStarted) {
            if (this == viewDebugTarget) println("refreshTheming abandoned due to not fullyStarted")
            return
        }
        if (parent?.fullyStarted == false) {

            if (this == viewDebugTarget) println("refreshTheming abandoned due to parent $parent not being fully started")
            return
        }
        val themeBorrowed = if(themeTakeNonCascadingFromParent) parent?.theme ?: Theme.Companion.placeholder
        else parent?.theme?.let { it.revert ?: it } ?: Theme.Companion.placeholder
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
        internalChildren.removeAt(index).also { it.shutdown() }
    }

    public fun removeChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call removeChild was made")
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

    public val loading: Signal<Boolean> = Signal(false)
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
    public val working: Signal<Boolean> = Signal(false)
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
            override fun working(reactive: Reactive<*>) {
                listenForWorking(reactive)
            }

            override fun loading(reactive: Reactive<*>) {
                listenForStatus(reactive)
            }
        })
        add(Dispatchers.Main.immediate)
    }
    public override val coroutineContext: CoroutineContext = contextSetup()

    fun withoutLoadingAnimations(): CoroutineContext = coroutineContext.minusKey(StatusListener.Key);

    internal fun listenForWorking(readable: Reactive<*>): () -> Unit {
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
                    (handle(myView) ?: ExceptionHandlers.Companion.root.handle(myView, true, it))?.let { excEnder = it }
                }
            }
        }
        onRemove(r)
        return r
    }

    internal fun listenForStatus(readable: Reactive<*>): () -> Unit {
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
                    (handle(myView) ?: ExceptionHandlers.Companion.root.handle(myView, false, it))?.let {
                        excEnder = it
                    }
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
        return (handle(myView) ?: ExceptionToMessages.Companion.root.handle(myView, exception))
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
        parent = null
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

    operator fun Action.invoke() = startAction(this@RViewHelper)
}