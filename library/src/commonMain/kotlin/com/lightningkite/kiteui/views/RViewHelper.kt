package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Core implementation class providing common view functionality for all RView instances.
 *
 * RViewHelper provides the complete view lifecycle including:
 * - **Hierarchy Management**: Parent/child relationships with proper lifecycle coordination
 * - **Theming**: Reactive theme application with cascading and derivation support
 * - **State Management**: Integrated working/loading state tracking via reactive primitives
 * - **Exception Handling**: Hierarchical exception handler chains with customizable messaging
 * - **Coroutines**: Built-in CoroutineScope with proper cancellation on shutdown
 * - **Resource Cleanup**: Automatic cleanup of listeners and child views on shutdown
 *
 * ## Lifecycle
 * 1. **Construction**: View is created with an [RContext]
 * 2. **Setup**: Properties are configured, children may be added
 * 3. **postSetup()**: Called when view is fully configured and added to parent
 * 4. **Active**: View is part of the hierarchy and responds to state/theme changes
 * 5. **shutdown()**: View is removed from hierarchy, all resources are cleaned up
 *
 * ## Threading
 * All view operations must occur on the main thread. The coroutine context is configured
 * with [Dispatchers.Main] to ensure proper thread affinity.
 *
 * ## Memory Management
 * Views use [SupervisorJob] for coroutines to prevent child failures from affecting siblings.
 * When [leakDetection] is enabled, views are monitored after shutdown to detect memory leaks.
 *
 * @param context The rendering context providing platform-specific configuration.
 */
@OptIn(InternalKiteUi::class)
abstract class RViewHelper(override val context: RContext) : ViewWriter() {
    abstract var showOnPrint: Boolean
    override val representsView: RView get() = this as RView

    /**
     * Flag indicating whether this view has been shut down.
     * Once true, operations on this view will log warnings and may not function correctly.
     */
    var isShutdown = false
        private set

    /** The opacity of this view, from 0.0 (fully transparent) to 1.0 (fully opaque). */
    open var opacity: Double = 1.0

    /**
     * Whether this view is shown in the layout. When false, the view is removed from layout
     * calculations (similar to CSS `display: none`).
     */
    open var shown: Boolean = true

    @Deprecated("Renamed to 'shown'", ReplaceWith("shown"))
    var exists: Boolean
        get() = shown
        set(value) { shown = value }

    /**
     * Whether this view is visible. When false, the view still occupies space in the layout
     * but is not rendered (similar to CSS `visibility: hidden`).
     */
    open var visible: Boolean = true

    /**
     * The spacing (gap) between child elements within this view.
     * If null, the theme's default gap is used.
     */
    open var gap: Dimension? = null

    @Deprecated("Renamed to 'gap'", ReplaceWith("gap"))
    var spacing: Dimension?
        get() = gap
        set(value) { gap = value }

    /** Whether this view should ignore all user interaction (pointer events, focus, etc.). */
    open var ignoreInteraction: Boolean = false

    /**
     * Uniform padding for all edges. Setting this will create an [Edges] instance with
     * the same value for all sides. Getting returns the left padding if set.
     */
    var padding: Dimension?
        get() = paddingByEdge?.left
        set(value) { paddingByEdge = value?.let(::Edges) }

    /**
     * Additional padding added on top of the base [paddingByEdge].
     * This is useful for temporary padding adjustments (e.g., for safe area insets).
     */
    var safeAreaPadding: Edges? = null
        set(value) {
            field = value
            refreshPadding()
        }

    /**
     * Explicit padding for each edge. If null and the theme has padding enabled,
     * the theme's padding will be used.
     */
    open var paddingByEdge: Edges? = null
        set(value) {
            field = value
            refreshPadding()
        }

    /**
     * Optional transition ID for shared element transitions.
     * Views with matching transition IDs can animate smoothly between screens/states.
     */
    open var transitionId: String? = null

    // Safe insets handling
    /** Cached weight value for layout calculations. */
    var lastSetWeight: Float? = null
    /** Cached horizontal alignment for layout calculations. */
    var lastSetHorizontalAlign: Align = Align.Stretch
    /** Cached vertical alignment for layout calculations. */
    var lastSetVerticalAlign: Align = Align.Stretch
    /** Default horizontal alignment for newly created children when not explicitly set. */
    open var newChildHorizontalAlign: Align? = null
    /** Default vertical alignment for newly created children when not explicitly set. */
    open var newChildVerticalAlign: Align? = null

    // drag 'n drop
    /** Data to be provided when this view is dragged. If null, dragging is disabled. */
    open var dragData: DragData? = null
    /** Delegate for handling drop events when items are dragged onto this view. */
    open var dropTargetDelegate: DropTargetDelegate? = null

    abstract fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
    abstract fun requestFocus()

    companion object {
        /**
         * Global flag to enable leak detection. When true, views are monitored after shutdown
         * to ensure they are garbage collected.
         */
        var leakDetection: Boolean = false

        /**
         * Global flag controlling child removal behavior during shutdown.
         * When true, children are explicitly removed from the native view hierarchy before shutdown.
         * This may help with platform-specific cleanup requirements.
         */
        var removeBeforeShutdown: Boolean = false
    }


    // Theming

    /**
     * When true, this view takes all theme properties (including non-cascading ones like
     * background and padding) from the parent theme instead of just cascading properties.
     * This is useful for nested interactive elements that should match their container's style.
     */
    var themeTakeNonCascadingFromParent: Boolean = false
        set(value) {
            field = value
            refreshTheming()
        }

    /**
     * The theme derivation function that determines how this view's theme is computed
     * from its parent's theme. The default is [ThemeDerivation.none] which passes through
     * the parent theme unchanged.
     */
    var themeChoice: ThemeDerivation = ThemeDerivation.Companion.none
        set(value) {
            field = value
            refreshTheming()
        }

    /** The currently active theme for this view. */
    val theme: Theme get() = themeAndBack.theme

    /**
     * The currently active theme along with background rendering flags.
     * This is computed from the parent theme and [themeChoice], then has state applied
     * via [applyState].
     */
    var themeAndBack: ThemeAndBack = Theme.Companion.placeholder.withBack
        private set(value) {
            if (value != field) {
                val oldCascading = field.theme.let { it.revert ?: it }
                field = value
                applyTheme(value)
                refreshPadding()
                val newCascading = value.theme.let { it.revert ?: it }
                if (oldCascading !== newCascading) {
                    for (child in internalChildren) {
                        child.refreshTheming()
                    }
                }
            }
        }

    /**
     * Returns the effective spacing that should be used between children of this view,
     * calculated as the minimum of padding and gap.
     */
    open val mySpacingForChildren: Dimension
        get()  {
            val pad = padding ?: themeAndBack.theme.padding.top
            val gap = gap ?: themeAndBack.theme.gap
            return minOf(pad, gap)
        }

    /**
     * Returns the final padding that should be applied to this view, combining
     * explicit padding, theme padding, and additional padding.
     */
    val appliedPadding get() = (paddingByEdge ?: if(themeAndBack.padding) theme.padding else Edges.ZERO).let {
        if(safeAreaPadding != null) it + safeAreaPadding
        else it
    }

    /**
     * Flag indicating whether [postSetup] has been called and the view is fully initialized.
     * Theme refreshes are deferred until this is true to avoid applying themes prematurely.
     */
    protected var fullyStarted = false

    abstract fun applyTheme(theme: ThemeAndBack)

    /**
     * Applies runtime state (working, loading) to the theme before rendering.
     * Subclasses can override this to apply additional semantic variations.
     *
     * @param theme The base theme to modify.
     * @return The theme with state semantics applied.
     */
    open fun applyState(theme: ThemeAndBack): ThemeAndBack = theme
        .let { if(working.value) it[WorkingSemantic] else it }
        .let { if(loading.value) it[LoadingSemantic] else it }

    /**
     * Called when padding values change. Subclasses should override this to apply
     * the new padding to their platform-specific views.
     */
    open fun refreshPadding() {
    }

    /**
     * Recomputes and applies the theme for this view based on parent theme and theme choice.
     * This is called automatically when theme-related properties change or when the parent's
     * theme changes.
     *
     * **GOTCHA**: Theme refresh is deferred until [fullyStarted] is true and the parent is
     * also fully started to ensure proper initialization order.
     */
    fun refreshTheming() {
        if (this == viewDebugTarget) println("refreshTheming")
        if (!fullyStarted) {
            if (this == viewDebugTarget) println("refreshTheming abandoned due to not fullyStarted")
            return
        }
        if (themeParent?.fullyStarted == false) {
            if (this == viewDebugTarget) println("refreshTheming abandoned due to themeParent $themeParent not being fully started")
            return
        }
        val themeBorrowed = if(themeTakeNonCascadingFromParent) themeParent?.theme ?: Theme.placeholder
        else themeParent?.theme?.let { it.revert ?: it } ?: Theme.placeholder
        if (this == viewDebugTarget) println("refreshTheming will set!  Parent theme is ${themeBorrowed.id}")
        val t = applyState(themeChoice(themeBorrowed))
        if (this == viewDebugTarget) println("refreshTheming will set to ${t.theme.id}!")
        themeAndBack = t
    }


    // Children

    /**
     * The parent view in the hierarchy. Setting this triggers theme refresh to inherit
     * the parent's theme. This should typically only be set by the parent's child management methods.
     */
    var parent: RView? = null
        @InternalKiteUi
        set(value) {
            field = value
            if (value != null) refreshTheming()
        }
    @InternalKiteUi
    var themeParentOverride: RView? = null
        set(value) {
            field = value
            if (value != null) refreshTheming()
        }
    @InternalKiteUi
    val themeParent get() = themeParentOverride ?: parent

    private val internalChildren = ArrayList<RView>()

    /**
     * Read-only list of child views. Use [addChild], [removeChild], or [clearChildren]
     * to modify the child list.
     */
    val children: List<RView> get() = internalChildren

    /**
     * Called before a child view is added. This sets the child's parent reference.
     * **GOTCHA**: This will log a warning if called on a shutdown view.
     */
    override fun willAddChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call willAddChild was made")
        view.parent = this as RView
    }

    /**
     * Adds a child view at a specific index in the child list.
     * This calls [internalAddChild] to update the platform-specific view hierarchy.
     *
     * @param index The position at which to insert the child (0-based).
     * @param view The child view to add.
     */
    fun addChild(index: Int, view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call addChild was made")
        if (view.parent !== this) view.parent = this as RView
        internalChildren.add(index, view)
        internalAddChild(index, view)
    }

    /**
     * Adds a child view at the end of the child list.
     * This is the primary method for adding children and is called by [ViewWriter.addChild].
     *
     * @param view The child view to add.
     */
    override fun addChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call addChild was made")
        if (view.parent !== this) view.parent = this as RView
        val index = children.size
        internalChildren.add(index, view)
        internalAddChild(index, view)
    }

    /**
     * Removes the child view at the specified index and shuts it down.
     * **IMPORTANT**: The removed child will have [shutdown] called on it automatically.
     *
     * @param index The index of the child to remove.
     * @throws IllegalArgumentException if the index is out of bounds.
     */
    fun removeChild(index: Int) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call removeChild was made")
        if (index !in children.indices) throw IllegalArgumentException("$index not in range ${children.indices}")
        internalRemoveChild(index)
        internalChildren.removeAt(index).also { it.shutdown() }
    }

    /**
     * Removes a specific child view and shuts it down.
     * **IMPORTANT**: The removed child will have [shutdown] called on it automatically.
     *
     * @param view The child view to remove.
     * @throws IllegalStateException if the view is not a child of this view.
     */
    fun removeChild(view: RView) {
        if(isShutdown) println("WARNING!! $this is shut down, but attempt to call removeChild was made")
        val i = children.indexOf(view)
        if (i != -1) removeChild(i)
        else {
            throw IllegalStateException("$view is not a child of $this!")
        }
    }

    /**
     * Removes all child views and shuts them down.
     * **IMPORTANT**: All removed children will have [shutdown] called on them automatically.
     */
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

    /**
     * Adds an exception handler to this view. Exceptions from reactive operations within
     * this view's scope will be handled by these handlers in the order they were added.
     * If no handler in this view handles the exception, it propagates to the parent's handlers.
     */
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

    /**
     * Adds an exception-to-message converter to this view. When exceptions occur, these
     * converters are used to produce user-friendly error messages. Converters are tried
     * in order, with fallback to parent converters if needed.
     */
    operator fun plusAssign(exceptionToMessage: ExceptionToMessage) {
        exceptionToMessages?.let {
            it += exceptionToMessage
        } ?: run {
            exceptionToMessages = ExceptionToMessages().apply {
                this += exceptionToMessage
            }
        }
    }

    /**
     * Signal indicating whether this view is currently in a loading state.
     * When true, the [LoadingSemantic] theme variation is applied.
     */
    private val _loading = Signal(false)
    val loading: ReactiveValue<Boolean> get() = _loading
    private var loadCount = 0
        set(value) {
            field = value
            if (value == 0 && _loading.value) {
                _loading.value = false
                refreshTheming()
            } else if (value > 0 && !_loading.value) {
                _loading.value = true
                refreshTheming()
            }
        }

    /**
     * Signal indicating whether this view is currently in a working state.
     * When true, the [WorkingSemantic] theme variation is applied.
     */
    private val _working = Signal(false)
    val working: ReactiveValue<Boolean> get() = _working
    private var workCount = 0
        set(value) {
            field = value
            if (value == 0 && _working.value) {
                _working.value = false
                refreshTheming()
            } else if (value > 0 && !_working.value) {
                _working.value = true
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
        // Use ssrDispatcher if set (for SSR synchronous execution), otherwise use Main dispatcher
        add(context.ssrDispatcher ?: Dispatchers.Main.immediate)
    }

    /**
     * The coroutine context for this view, configured with:
     * - [SupervisorJob] for independent child coroutine failure handling
     * - [CoroutineExceptionHandler] for logging unhandled exceptions
     * - [StatusListener] for automatic working/loading state tracking
     * - [Dispatchers.Main] for main thread execution
     */
    override val coroutineContext: CoroutineContext = contextSetup()

    /**
     * Returns a coroutine context without the [StatusListener], useful when you want to
     * perform reactive operations without triggering loading animations.
     */
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

    /**
     * Converts an exception to a user-friendly error message using the exception-to-message
     * converters registered on this view and its ancestors.
     *
     * @param exception The exception to convert.
     * @return A user-friendly error message, or null if no converter could handle the exception.
     */
    fun exceptionToMessage(exception: Exception): ExceptionMessage? {
        val myView = this@RViewHelper as RView
        fun handle(view: RViewHelper): ExceptionMessage? {
            return view.exceptionToMessages?.handle(myView, exception) ?: view.parent?.let { handle(it) }
        }
        return (handle(myView) ?: ExceptionToMessages.Companion.root.handle(myView, exception))
    }

    // Cleanup Insurance

    /**
     * Shuts down this view, canceling all coroutines, removing all children, and clearing
     * parent references.
     *
     * **IMPORTANT**: After shutdown, this view should not be used and will log warnings if
     * child operations are attempted.
     *
     * The shutdown process:
     * 1. Cancels the coroutine job, stopping all reactive listeners
     * 2. Recursively shuts down all children
     * 3. Optionally checks for memory leaks if [leakDetection] is enabled
     * 4. Marks the view as shut down and clears parent reference
     */
    @InternalKiteUi
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
        parent = null
    }

    /**
     * Initiates leak detection for this view by creating a weak reference and checking
     * after a delay whether the view has been garbage collected.
     */
    @InternalKiteUi
    open fun leakDetect() {
        WeakReference(this).checkLeakAfterDelay(1000)
    }

    protected abstract fun internalAddChild(index: Int, view: RView)
    protected abstract fun internalRemoveChild(index: Int)
    protected abstract fun internalClearChildren()

    /**
     * Called after the view has been fully configured and added to its parent.
     * This marks the view as [fullyStarted] and triggers theme application.
     *
     * **IMPORTANT**: Theme changes are deferred until this is called to ensure proper
     * initialization order.
     */
    @InternalKiteUi
    open fun postSetup() {
        fullyStarted = true
        refreshTheming()
    }

    abstract fun screenRectangle(): Rect?


    // Calculation context

    @Deprecated("Not needed anymore", ReplaceWith("this"))
    val calculationContext: CoroutineScope get() = this

    /**
     * Optional debug name for this view. When null, [toString] generates a name based on
     * the theme ID and class name.
     */
    open var debugName: String? = null

    /**
     * Returns a string representation of this view for debugging purposes.
     * Uses [debugName] if set, otherwise generates a name from theme and class information.
     */
    override fun toString(): String {
        return debugName ?: (theme.id + " " + this::class.toString().removePrefix("class ") + "@" + this.identityHashCode().toString(16))
    }

    /**
     * Optional HTML element ID for web platforms. This allows targeting specific views
     * with CSS or JavaScript when rendered to HTML.
     */
    open var htmlElementId: String? = null

    // by Claude - allows setting semantic HTML tag from common code for SEO
    open var htmlElementTag: String? = null

    /**
     * Convenience operator allowing actions to be invoked with this view as the context.
     * Example: `myAction()`
     */
    operator fun Action.invoke() = startAction(this@RViewHelper)
}

/*
TODO: API Improvement Recommendations for RViewHelper

1. **State Management Consolidation**: The `loadCount` and `workCount` pattern is error-prone.
   Consider using an atomic counter or a more explicit state machine to avoid race conditions
   in multi-threaded scenarios.

2. **Exception Handler Builder**: Provide a DSL-style builder for exception handlers:
   ```kotlin
   exceptionHandling {
       handle<NetworkException> { showToast("Network error") }
       handle<ValidationException> { showValidationErrors(it) }
   }
   ```

3. **Padding API Simplification**: The three-level padding system (paddingByEdge, additionalPadding, theme padding)
   is complex. Consider a clearer API:
   - `basePadding: Edges?` - explicit padding
   - `safePadding: Edges?` - safe area insets
   - `fun effectivePadding(): Edges` - computed result

4. **Theme Refresh Optimization**: The `refreshTheming()` method cascades to all children even when
   only non-cascading properties change. Consider:
   - Track which properties changed (cascading vs non-cascading)
   - Only refresh children when cascading properties change
   - Add `refreshThemingLocal()` for local-only updates

5. **Lifecycle State Enum**: Replace boolean flags (`isShutdown`, `fullyStarted`) with a proper lifecycle enum:
   ```kotlin
   enum class ViewLifecycle { CREATED, SETUP_PENDING, ACTIVE, SHUTDOWN }
   ```

6. **Child Management**: The warning logs for operations on shutdown views should be structured logging
   with stack traces for debugging. Consider throwing exceptions in debug mode instead of just logging.

7. **CoroutineContext Builder**: Make context setup more extensible by allowing subclasses to contribute
   to the context via a protected open method like `contributeToContext(MutableCoroutineContext)`.

8. **Memory Leak Detection**: The global `leakDetection` flag is crude. Consider per-view or per-hierarchy
   configuration, and provide callbacks when leaks are detected rather than just logging.
*/