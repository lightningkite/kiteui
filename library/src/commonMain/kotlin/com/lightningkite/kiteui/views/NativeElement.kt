@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.telemetry.TelemetryContext
import com.lightningkite.kiteui.utils.OrderedKeyedList
import com.lightningkite.reactive.context.StatusListener
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline

/**
 * Platform-specific native element implementation.
 *
 * This is the base class for all platform-native UI elements in KiteUI. Each platform provides
 * its own implementation that wraps the platform's native view type:
 * - **Android**: Wraps `android.view.View`
 * - **iOS**: Wraps `UIView`
 * - **JS/Web**: Wraps `HTMLElement`
 * - **JVM/Swing**: Wraps `JComponent`
 *
 * ## Architecture
 *
 * [NativeElement] sits between the pure [Element] interface and platform-specific code:
 * 1. **[Element]** - Pure interface defining what all elements must provide
 * 2. **[NativeElementCommonCode]** - Shared cross-platform logic (lifecycle, theming, etc.)
 * 3. **[NativeElement]** (this class) - Platform-specific rendering and view management
 * 4. **Concrete elements** - Actual components like `TextView`, `Button`, `RowOrCol`, etc.
 *
 * ## When to Extend NativeElement
 *
 * Extend [NativeElement] when you're creating a new platform-specific UI component:
 * - Custom views that don't exist in KiteUI
 * - Wrappers around third-party native views
 * - Platform-specific optimizations
 *
 * For containers that hold children, extend [NativeContainerElement] instead.
 *
 * ## Creating a Custom Element
 *
 * ### Step 1: Common Declaration
 * ```kotlin
 * // In commonMain
 * expect class CustomGauge(context: ElementContext) : NativeElement {
 *      var value: Double
 * }
 *
 * // DSL function for creating it
 * inline fun ElementWriter.customGauge(setup: CustomGauge.() -> Unit = {}): CustomGauge =
 *     write(CustomGauge(context), setup)
 * ```
 *
 * ### Step 2: Platform-Specific Declarations
 *
 * We'll show a very simplified android implementation here just as an example.
 *
 * ```kotlin
 * // In androidMain
 * actual class CustomGauge actual constructor(context: ElementContext) : NativeElement(context) {
 *
 *     // The actual Android View
 *     override val native = object : View(context.activity) {
 *         override fun onDraw(canvas: Canvas) {
 *             // Custom drawing
 *         }
 *     }
 *
 *     // Public API
 *     actual var value: Double = 0.0
 *         set(v) {
 *             field = v
 *             native.invalidate()
 *         }
 * }
 * ```
 *
 * Each platform's [NativeElement] has different requirements for implementation.
 *
 * ## Key Methods to Implement
 *
 * Platform implementations must provide:
 * - `val native` - The platform's native view (View/UIView/HTMLElement/etc.)
 * - [nativeApplyTheme] - Apply theme colors, fonts, etc. to the native view
 * - [refreshPadding] - Update the native view's padding
 * - [scrollIntoView] - Scroll this view into visible area
 * - [requestFocus] - Give this view keyboard focus
 *
 * You also need to implement the property setters (like [opacity], [shown], etc.)
 * which translate KiteUI properties to platform-specific view properties.
 *
 * ## What NativeElementCommonCode Provides
 *
 * You get these for free from [NativeElementCommonCode]:
 * - **Lifecycle management** - [onStartup]/[onShutdown] with proper coroutine scoping
 * - **Theme pipeline** - Automatic theme calculation from parent + modifiers + state
 * - **Process tracking** - Loading/working state aggregation
 * - **Reactive support** - Coroutine scope and process watchers
 * - **Debug helpers** - Leak detection, logging, toString()
 *
 * ## Example: TextView (Simplified)
 *
 * ```kotlin
 * // Common
 * expect class TextView(context: ElementContext) : NativeElement {
 *     var content: String
 * }
 *
 * // Android
 * actual class TextView actual constructor(context: ElementContext) :
 *     NativeElement(context) {
 *
 *     override val native = android.widget.TextView(context.activity)
 *
 *     actual var content: String
 *         get() = native.text.toString()
 *         set(value) { native.text = value }
 *
 *     override fun nativeApplyTheme(theme: ThemeAndBack) {
 *         native.setTextColor(theme.theme.foreground.colorInt())
 *         // ... other theme properties
 *     }
 *
 *     override fun refreshPadding() {
 *         val p = appliedPadding
 *         native.setPadding(p.left.px, p.top.px, p.right.px, p.bottom.px)
 *     }
 * }
 * ```
 *
 * ## Important Notes
 *
 * - **Thread safety**: Native views must be accessed on the main/UI thread only
 * - **Don't call super for expect/actual overrides**: The properties in this expect class
 *   don't have implementations to call super on
 * - **Use existing elements**: Check if a similar element already exists before creating new ones
 * - **Platform parity**: Try to keep behavior consistent across platforms
 *
 * @see NativeElementCommonCode for shared implementation details
 * @see NativeContainerElement for elements that contain children
 * @see Element for the base interface
 */
public expect abstract class NativeElement(context: ElementContext) : Element, NativeElementCommonCode {
    override var opacity: Double

    override var shown: Boolean

    override var visible: Boolean

    override var ignoreInteraction: Boolean

    override var dragData: DragData?

    override var dropTargetDelegate: DropTargetDelegate?

    /**
     * Applies theme to the native view (platform implementation).
     *
     * Called automatically when the element's theme changes. Platform implementations
     * should update native view properties to match the theme:
     * - Colors (background, foreground, borders)
     * - Typography (font, size, weight)
     * - Spacing (if not handled by padding)
     * - Corner radii
     * - Elevation/shadows
     *
     * Example (Android):
     * ```kotlin
     * override fun nativeApplyTheme(theme: ThemeAndBack) {
     *     if (theme.drawBackground) {
     *         native.background = theme.theme.backgroundDrawable()
     *         native.elevation = theme.theme.elevation.px
     *     }
     *     native.setTextColor(theme.theme.foreground.colorInt())
     * }
     * ```
     */
    override fun nativeApplyTheme(theme: ThemeAndBack)

    /**
     * Updates the native view's padding (platform implementation).
     *
     * Called when [Element.paddingByEdge] or [Element.safeAreaPadding] changes.
     * Platform implementations should apply the padding from [appliedPadding].
     *
     * Example (Android):
     * ```kotlin
     * override fun refreshPadding() {
     *     val p = appliedPadding
     *     native.setPadding(p.left.px, p.top.px, p.right.px, p.bottom.px)
     * }
     * ```
     */
    override fun refreshPadding()

    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)

    override fun requestFocus()

    /**
     * Returns the screen-relative rectangle occupied by this element.
     *
     * Used for positioning popups, tooltips, etc. relative to the screen.
     * Returns null if the element is not currently laid out or visible.
     *
     * @return Rectangle in screen coordinates, or null if unavailable
     */
    public fun screenRectangle(): Rect?

    /**
     * Returns the parent-relative rectangle occupied by this element.
     *
     * The rectangle is relative to the parent container's coordinate space.
     * Returns null if the element is not currently laid out.
     *
     * @return Rectangle in parent coordinates, or null if unavailable
     */
    public fun parentRectangle(): Rect?

    override var showOnPrint: Boolean
}

/**
 * Shared platform-independent code for all native elements.
 *
 * This is only directly inherited by [NativeElement]. Any instance of this class is also a [NativeElement].
 *
 * # Implementation Details
 *
 * This abstract class provides common implementation for functionality that works the same
 * across all platforms. Platform-specific [NativeElement] implementations inherit from this
 * to get these features for free.
 *
 * ## What This Class Provides
 *
 * ### 1. Lifecycle Management
 * - Manages element startup and shutdown states
 * - Provides coroutine scope tied to element lifetime
 * - Handles exceptions in element coroutines
 * - Automatic cleanup on shutdown
 *
 * ### 2. Theme Pipeline
 * The sophisticated theme system that combines:
 * - **Base theme** from parent (cascading or non-cascading)
 * - **User choice** from modifiers (`card`, `important`, etc.)
 * - **Dynamic choice** from `dynamicThemed`
 * - **Element status** (selected, checked, disabled)
 * - **Processing status** (loading, working, error)
 *
 * Themes are applied in order and combined to produce the final [themeAndBack].
 *
 * ### 3. Reactive Process Tracking
 * Automatically tracks background and foreground processes:
 * - **Background processes** (loading data) → applies [LoadingSemantic] when not ready
 * - **Foreground processes** (button clicks) → applies [WorkingSemantic] when active
 * - Aggregates multiple processes into single state
 * - Handles exceptions and shows them in UI
 *
 * ### 4. Debug & Development Tools
 * - Debug logging with [debug] function
 * - Leak detection in debug mode
 * - Human-readable [toString]
 * - Support for [debugName]
 *
 * ## Element Delegation Pattern
 *
 * This class supports element delegation (wrapping). You can create wrapper elements:
 *
 * ```kotlin
 * class HighlightableText(inner: TextView) : Element by inner {
 *     var highlighted: Boolean = false
 *         set(value) {
 *             field = value
 *             // Access inner element's properties
 *             themeChoice = if (value) ImportantSemantic else ThemeDerivation.None
 *         }
 * }
 * ```
 *
 * The [outermostElement] field tracks the outermost wrapper, so the framework knows
 * to use the wrapper when needed (e.g., for theme calculations, debug names, etc.).
 *
 * ## Coroutine Scope & Exception Handling
 *
 * Elements have their own coroutine scope that:
 * - Starts when [onStartup] is called
 * - Cancels when [onShutdown] is called
 * - Uses a [SupervisorJob] so one failed coroutine doesn't kill others
 * - Reports exceptions via [ElementContext.handleException]
 *
 * ```kotlin
 * myElement.launch {
 *     // Runs in element's scope
 *     // Automatically cancelled on shutdown
 * }
 * ```
 *
 * ## Theme Pipeline Details
 *
 * The theme pipeline has ordered steps (see [ThemePipeline.Step]):
 *
 * 1. **Element styling** (0.0) - Element's own default theme
 * 2. **User choice** (0.2) - Static modifiers like `card`
 * 3. **Dynamic choice** (0.4) - Reactive `dynamicThemed`
 * 4. **Element status** (0.6) - Element state (selected, checked)
 * 5. **Processing status** (0.8) - Loading/working/error states
 *
 * Each step can add theme derivations, and they combine in order. The final theme
 * is applied to the native view via [nativeApplyTheme].
 *
 * ## Performance Considerations
 *
 * **This code is duplicated in every element in the view tree**, so performance matters:
 * - Properties are carefully designed to minimize allocations
 * - Theme calculations are lazy where possible
 * - Process tracking uses efficient bit-based severity tracking
 * - Debug code is behind inline functions that optimize away in release builds
 *
 * ## You Should Not Extend This Directly
 *
 * **Don't extend [NativeElementCommonCode] directly.** Instead:
 * - Extend [NativeElement] for custom platform-specific elements
 * - Extend [NativeContainerElement] for custom containers
 * - Use delegation for wrapper elements
 *
 * Platform-specific [NativeElement] implementations automatically inherit this class
 * through the expect/actual mechanism.
 *
 * @see NativeElement for creating custom elements
 * @see Element for the public interface
 * @see ThemePipeline for theme system details
 */
public abstract class NativeElementCommonCode internal constructor(override val context: ElementContext) : Element {
    // This code is duplicated in every element throughout the entire view tree, so performance actually kinda matters.

    override val underlyingNativeElement: NativeElement get() = this as NativeElement

    /**
     * The outermost wrapper element when using delegation, or `this` if not wrapped.
     *
     * ## The Element Delegation Pattern
     *
     * KiteUI supports wrapping elements via Kotlin delegation:
     * ```kotlin
     * class HighlightableText(inner: TextView) : Element by inner {
     *     var highlighted: Boolean = false
     *         set(value) {
     *             field = value
     *             themeChoice = if (value) ImportantSemantic else ThemeDerivation.None
     *         }
     * }
     * ```
     *
     * When you use delegation, there are two elements:
     * - **Inner element** (the `TextView`) - Provides the actual native view
     * - **Wrapper element** (the `HighlightableText`) - Adds additional functionality
     *
     * ## The Problem
     *
     * With delegation, calling `element.someProperty` on the wrapper calls the inner element's
     * implementation. This is great for most cases, but causes issues when the framework needs
     * to know about the wrapper:
     *
     * - **Theme calculations** - Should use the wrapper's `themeChoice`, not the inner element's
     * - **Debug names** - Should show the wrapper's `debugName`, not the inner element's
     * - **toString()** - Should describe the wrapper, not the inner element
     * - **Exception handling** - Should report the wrapper as the source
     *
     * ## The Solution
     *
     * [outermostElement] tracks the outermost wrapper. It's set automatically by
     * [ElementWriter.write] when the element is added to the view tree:
     *
     * ```kotlin
     * fun <T : Element> ElementWriter.write(element: T, setup: T.() -> Unit): T {
     *     element.ensureOutermostElement()  // Sets outermostElement = wrapper
     *     willAddChild(element)
     *     setup(element)
     *     element.onStartup()
     *     addChild(element)
     *     return element
     * }
     * ```
     *
     * ## How It's Used Internally
     *
     * Framework code uses [outermostElement] when it needs the wrapper:
     * ```kotlin
     * override fun toString(): String =
     *     outermostElement.debugName ?: (/* auto-generated from outermostElement */)
     *
     * fun refreshTheming() {
     *     val t = themePipeline.apply(outermostElement, base)  // Use wrapper's theme
     *     // ...
     * }
     * ```
     *
     * ## When You Need to Know About This
     *
     * **Most developers never need to think about this.** It's handled automatically.
     *
     * You might care if you're:
     * - Creating element wrappers via delegation (it just works automatically)
     * - Debugging why an element's properties seem "off" (check if it's wrapped)
     * - Working on KiteUI internals (use `outermostElement` when you need the wrapper)
     *
     * ## For Framework Developers
     *
     * When implementing framework code, remember:
     * - Use `outermostElement` when you need the user-visible wrapper
     * - Use `this` when you need the actual native implementation
     * - Properties that users set (like `themeChoice`, `debugName`) should reference `outermostElement`
     *
     * Also, it's best to not rely on this being set correctly for any critical functionality.
     */
    internal open var outermostElement: Element = this

    override var parent: ContainerElement? = null
        internal set(value) {
            debug { "setting parent to $value" }
            field = value
            if (value != null) refreshTheming()
        }

    // ---- LIFECYCLE ---

    init {
        if (Element.Debugger.countInstances) Element.Debugger.recordCreated(this)
    }

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext = coroutineContextOf(
        job,
        CoroutineExceptionHandler { ctx, thr ->
            if (thr is CancellationException) return@CoroutineExceptionHandler

            thr.report(this.toString())

            if (thr is Exception) context.handleException(
                thr,
                ExceptionHandler.Metadata(
                    source = outermostElement,
                    process = null,
                    foregroundProcess = null,
                    context = mapOf("ctx" to ctx.toString())
                )
            )
        },
        context.ssrDispatcher ?: Dispatchers.Main.immediate,
        // A *separate* StatusListener object, not `this`: the element must not be the object returned
        // by coroutineContext[StatusListener], or its CoroutineScope identity and its StatusListener
        // identity would be the same object, making `element.job` ambiguous (see Element.kt). Element
        // is no longer a StatusListener; this object delegates to the element's own process watching.
        object : StatusListener {
            override fun watchBackgroundProcess(status: Reactive<*>): Release =
                this@NativeElementCommonCode.watchBackgroundProcess(status)
            override fun watchForegroundProcess(status: Reactive<*>): Release =
                this@NativeElementCommonCode.watchForegroundProcess(status)
        },
        TelemetryContext(element = this as NativeElement)
    )

    public var fullyStarted: Boolean = false
        private set

    @OverrideOnly
    override fun onStartup() {
        if (fullyStarted) return
        fullyStarted = true
        debug { "starting..." }
        refreshTheming()
        debug { "new theme: ${theme.id}" }
    }

    public var isShutdown: Boolean = false
        private set

    @OverrideOnly
    override fun onShutdown() {
        if (isShutdown) return
        job.cancel()
        isShutdown = true
        if (Element.Debugger.countInstances) Element.Debugger.recordShutdown(this)
        if (Element.Debugger.leakDetect) leakDetect()
        parent = null
        // Cross-element accessibility associations hold strong references to arbitrary elements;
        // clearing them on shutdown prevents a dead element from retaining a whole other subtree.
        labelFor = null
        describedBy = null
    }


    // --- THEMING ---

    protected abstract fun nativeApplyTheme(theme: ThemeAndBack)
    public abstract fun refreshPadding()

    override var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
        protected set(value) {
            if (value == field) return
            field = value
            nativeApplyTheme(value)
            refreshPadding()
        }

    final override var paddingByEdge: Edges? = null
        set(value) {
            field = value
            debug { "Setting padding to $value" }
            refreshPadding()
        }

    final override var safeAreaPadding: Edges? = null
        set(value) {
            field = value
            refreshPadding()
        }

    /** The final computed padding combining theme padding, custom padding, and safe area insets */
    public val appliedPadding: Edges get() {
        if (!fullyStarted) Log.warn("$this attempted to calculate applied padding before fully started.")
        return (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding } ?: Edges.ZERO).let { p ->
            safeAreaPadding?.let { p + it } ?: p
        }
    }

    // Theme pipeline

    /**
     * Strategy for determining the base theme of an element.
     * Default strategy inherits cascading theme from parent.
     */
    public fun interface GetBaseTheme {
        public fun get(element: Element): Theme

        public companion object {
            public val fromParent: GetBaseTheme = GetBaseTheme { element ->
                element.parent?.theme?.let { it.revert ?: it } ?: Theme.placeholder
            }
            public val fromParentNonCascading: GetBaseTheme = GetBaseTheme { element -> element.parent?.theme ?: Theme.placeholder }
        }
    }

    @ExperimentalKiteUi
    public var themeBase: GetBaseTheme = GetBaseTheme.fromParent
        set(value) {
            field = value
            refreshTheming()
        }

    @ExperimentalKiteUi
    public var themePipeline: ThemePipeline = ThemePipeline(
        ThemePipeline.Step.processingStatus to ThemePipeline.ThemeForElement.processingTheming
    )

    final override var themeChoice: ThemeDerivation
        get() = themePipeline.get(ThemePipeline.Step.themeChoice, outermostElement)
        set(value) {
            themePipeline.set(ThemePipeline.Step.themeChoice, value)
            refreshTheming()
        }

    /**
     * Recalculates and applies the element's theme.
     *
     * Combines the base theme (typically from parent), theme choice (semantic modifiers like 'important'),
     * and state-based theming (loading/error states) to produce the final theme.
     */
    public fun refreshTheming() {
        debug { "refreshTheming" }
        if (!checkIsActive("refreshTheming")) return
        if (parent?.underlyingNativeElement?.currentlyActive() == false) {
            debug { "abandoning refreshTheming because parent $parent not started" }
            return
        }
        val base = themeBase.get(this as NativeElement)
        debug {
            val source = when (themeBase) {
                GetBaseTheme.fromParent -> ""
                GetBaseTheme.fromParentNonCascading -> "Pulling non-cascading theme from parent."
                else -> "Pulling base theme from non-standard method."
            }
            "refreshTheming will set! $source Base theme is ${base.id}"
        }
        val t = themePipeline.apply(outermostElement, base)
        debug { "refreshTheming will set to ${t.theme.id}!" }
        themeAndBack = t
    }


    // --- PROCESSING ---

    private inner class Processes(private val foreground: Boolean) : BaseListenable(), Reactive<Unit> {
        private val processes = HashSet<Reactive<*>>()

        private var exceptionCount = 0
        private var notReadyCount = 0

        private var releaseExceptionHandler: Release? = null

        override var state: ReactiveState<Unit> = ReactiveState(Unit)
            private set(value) {
                if (field.raw !== value.raw) {
                    field = value
                    invokeAllListeners()
                    refreshTheming()
                }
            }

        private fun recalculateState() {
            if (!currentlyActive()) return
            state = when {
                exceptionCount > 0 -> {
                    val pair = processes.firstNotNullOfOrNull { p -> p.state.exception?.let { p to it } }
                    if (pair == null) {
                        exceptionCount = 0 // recurse with new (accurate) exception count and then return because we already calculated it
                        return recalculateState()
                    }
                    val (process, exception) = pair

                    releaseExceptionHandler?.invoke()
                    releaseExceptionHandler = context.handleException(
                        exception,
                        ExceptionHandler.Metadata(
                            source = this@NativeElementCommonCode,  // TODO: Get wrapper element somehow
                            process = process,
                            foregroundProcess = foreground
                        )
                    )

                    ReactiveState.exception(exception)
                }

                notReadyCount > 0 -> {
                    releaseExceptionHandler?.invoke()
                    releaseExceptionHandler = null
                    ReactiveState.notReady
                }

                else -> {
                    releaseExceptionHandler?.invoke()
                    releaseExceptionHandler = null
                    ReactiveState(Unit)
                }
            }
        }

        private inline val ReactiveState<*>.severity
            get() =
                if (exception != null) SEV_EXCEPTION
                else if (!ready) SEV_NOT_READY
                else SEV_OK

        fun watch(status: Reactive<*>): Release {
            if (!processes.add(status)) return Listenable.Never.NOOP_RELEASE    // we are already listening to this status

            var prevSeverity: Int = -1  // intentionally using -1 as sentinel value to avoid boxing

            val release = status.addAndRunListener {
                onMainThread {
                    val s = status.state.severity
                    if (prevSeverity != s) {    // severity changed
                        when (prevSeverity) {
                            SEV_EXCEPTION -> exceptionCount--
                            SEV_NOT_READY -> notReadyCount--
                        }
                        when (s) {
                            SEV_EXCEPTION -> exceptionCount++
                            SEV_NOT_READY -> notReadyCount++
                        }
                        prevSeverity = s
                        recalculateState()
                    } else if (s == SEV_EXCEPTION) recalculateState() // exception changed
                }
            }

            return {
                release()
                if (processes.remove(status)) when (prevSeverity) { // dependency removed, clear load count
                    SEV_EXCEPTION -> {
                        exceptionCount--
                        recalculateState()
                    }
                    SEV_NOT_READY -> {
                        notReadyCount--
                        recalculateState()
                    }
                }
            }
        }
    }

    private val internalBackgroundProcesses = Processes(foreground = false)
    private val internalForegroundProcesses = Processes(foreground = true)

    // Plain methods now (Element is no longer a StatusListener); the StatusListener object in
    // coroutineContext delegates here.
    public fun watchBackgroundProcess(status: Reactive<*>): Release = internalBackgroundProcesses.watch(status).also(::onRemove)
    public fun watchForegroundProcess(status: Reactive<*>): Release = internalForegroundProcesses.watch(status).also(::onRemove)

    /** Aggregate state of background processes (data loading, etc.) - affects loading semantics */
    public val backgroundProcesses: Reactive<*> get() = internalBackgroundProcesses

    /** Aggregate state of foreground processes (button clicks, etc.) - affects working/error semantics */
    public val foregroundProcesses: Reactive<*> get() = internalForegroundProcesses


    // --- DEBUGGING & DRIVER ---

    // --- ACCESSIBILITY ---
    override var accessibleLabel: String? = null
    override var accessibleLiveRegion: LiveRegionMode = LiveRegionMode.None
    override var labelFor: Element? = null
    override var describedBy: Element? = null

    override var debugName: String? = null

    override fun toString(): String =
        outermostElement.debugName ?: (theme.id + ' ' + outermostElement::class.toString().removePrefix("class ") + "&" + outermostElement.identityHashCode().toString(16))

    @InternalKiteUi
    public open fun leakDetect() {
        WeakReference(this).checkLeakAfterDelay(1000)
    }

    @InternalKiteUi
    public inline fun debug(requireTarget: Boolean = true, text: () -> String) {
        if ((!requireTarget && debugMode) || Element.Debugger.debugTarget?.underlyingNativeElement === this) Log.tag("$this DEBUG").info(text())
    }

    public fun currentlyActive(): Boolean = fullyStarted && !isShutdown

    @InternalKiteUi
    public fun checkIsShutdown(name: String): Boolean {
        if (isShutdown) {
            // Surface via the logging system rather than a bare println. This still returns true so
            // callers no-op; a teardown race (a reactive scope firing mid-disposal) can legitimately
            // reach here, so we warn loudly rather than throw. See viewPath for the offending element.
            Log.tag("NativeElement").warn("$name called on shut-down element ${viewPath()}")
            return true
        }
        return false
    }

    @InternalKiteUi
    public fun checkIsActive(name: String, requireTarget: Boolean = true): Boolean {
        if (checkIsShutdown(name)) return false
        if (!fullyStarted) {
            debug(requireTarget) { "$name abandoned due to not fully started" }
            return false
        }
        return true
    }

    /**
     * The theme pipeline system that combines multiple sources of theming.
     *
     * Themes in KiteUI come from multiple sources that need to be combined in a specific order.
     * The [ThemePipeline] manages this combining process to produce the final element theme.
     *
     * ## Theme Sources (in order)
     *
     * The pipeline has ordered steps (see [Step]). Lower numbers are applied first:
     *
     * 1. **Element styling** (0.0) - What the element chooses for itself
     *    - Example: A button might default to [ButtonSemantic]
     *
     * 2. **User choice** (0.2) - Static theme modifiers applied by the user
     *    - Example: `card.important.button { }`
     *    - Set via [themeChoice]
     *
     * 3. **Dynamic choice** (0.4) - Reactive theme modifiers
     *    - Example: `dynamicThemed { if (isSelected()) ImportantSemantic else None }`
     *    - Changes reactively based on conditions
     *
     * 4. **Element status** (0.6) - Themes from element's internal state
     *    - Example: A `ToggleButton` applies `SelectedSemantic` when checked
     *    - Set by the element itself based on its state
     *
     * 5. **Processing status** (0.8) - Loading/working/error states
     *    - Example: [LoadingSemantic] when background data is loading
     *    - Example: [WorkingSemantic] when button is processing a click
     *    - Applied automatically based on [backgroundProcesses] and [foregroundProcesses]
     *
     * ## How It Works
     *
     * Each step can have an [ThemeForElement] that produces a [ThemeDerivation]. Operations are
     * combined in order to create the final theme:
     *
     * ```kotlin
     * val finalTheme = baseTheme
     *     .derive(elementStyling)      // Step 1
     *     .derive(userChoice)          // Step 2
     *     .derive(dynamicChoice)       // Step 3
     *     .derive(elementStatus)       // Step 4
     *     .derive(processingStatus)    // Step 5
     * ```
     *
     * ## Adding to the Pipeline
     *
     * ```kotlin
     * element.themePipeline.add(Step.elementStatus, CheckedSemantic)
     * element.themePipeline.set(Step.dynamicChoice) { if (highlighted) Important else None }
     * ```
     *
     * **Use `add`** to combine with existing operations at a step.
     * **Use `set`** to replace the operation at a step.
     *
     * @see ThemeDerivation for how themes are derived
     * @see NativeElementCommonCode.refreshTheming for where this is used
     */
    public class ThemePipeline(private val operations: OrderedKeyedList<Step, ThemeForElement>) {
        public constructor(vararg init: Pair<Step, ThemeForElement>) : this(
            OrderedKeyedList(
                init.mapTo(ArrayList()) { OrderedKeyedList.Entry(it.first, it.second) }
            )
        )

        /**
         * A step in the theme pipeline with an ordering priority.
         *
         * Steps with lower [order] values are applied first. The order determines
         * which theme modifications override others.
         *
         * You can create and use your own steps, just make sure the [order] value
         * is unique.
         */
        @JvmInline
        public value class Step(public val order: Float) : Comparable<Step> {
            override fun compareTo(other: Step): Int = order.compareTo(other.order)

            public companion object {
                /** Element's default styling - what the element chooses for itself */
                public val elementStyling: Step = Step(0f)

                /**
                 * User's static theme choice via modifiers like `card`, `important`
                 *
                 * Use of this step is reserved for setting [Element.themeChoice]
                 * */
                internal val themeChoice = Step(0.2f)

                /**
                 * User's dynamic theme choice via `dynamicThemed { ... }`
                 *
                 * Use of this step is reserved for `dynamicThemed` applications.
                 * */
                internal val dynamicChoice = Step(0.4f)

                /** Themes from element's internal state (checked, selected, disabled, etc.) */
                public val elementStatus: Step = Step(0.6f)

                /** Automatic theming from loading/working/error states */
                public val processingStatus: Step = Step(0.8f)
            }
        }

        public fun interface ThemeForElement {
            public fun get(element: Element): ThemeDerivation

            public data class Constant(val theme: ThemeDerivation) : ThemeForElement {
                override fun get(element: Element): ThemeDerivation = theme
            }

            public companion object {
                /**
                 * The standard processing status theming operation.
                 *
                 * Automatically applied to all elements at [Step.processingStatus].
                 * Checks the element's [backgroundProcesses] and [foregroundProcesses]
                 * and applies:
                 * - [LoadingSemantic] when background processes are loading
                 * - [WorkingSemantic] when foreground processes are active
                 *
                 * This gives automatic visual feedback for loading states and button clicks.
                 */
                public val processingTheming: ThemeForElement = ThemeForElement { element ->
                    val element = element.underlyingNativeElement
                    val t = element.foregroundProcesses.state.handle(
                        success = { ThemeDerivation.None },
                        notReady = { WorkingSemantic },
                        exception = { WorkingSemantic }
                    )
                    if (!element.backgroundProcesses.state.success) t + LoadingSemantic else t
                }
            }
        }

        private fun List<OrderedKeyedList.Entry<Step, ThemeForElement>>.foldOn(element: Element): ThemeDerivation =
            fold(null) { acc: ThemeDerivation?, (_, op) ->
                when {
                    acc == null -> op.get(element)
                    else -> acc + op.get(element)
                }
            } ?: ThemeDerivation.None

        public fun get(step: Step, element: Element): ThemeDerivation = operations.get(step).foldOn(element)

        public fun get(element: Element): ThemeDerivation = operations.foldOn(element)

        public fun apply(element: Element, theme: Theme): ThemeAndBack =
            operations.fold(null) { acc: ThemeAndBack?, (_, op) ->
                when {
                    acc == null -> op.get(element).invoke(theme)
                    else -> acc + op.get(element)
                }
            } ?: theme.withoutBack


        public fun add(step: Step, op: ThemeForElement): Unit = operations.add(step, op)
        public fun add(step: Step, theme: ThemeDerivation): Unit = operations.add(step, ThemeForElement.Constant(theme))

        public fun set(step: Step, op: ThemeForElement): Unit = operations.set(step, op)

        public fun set(step: Step, theme: ThemeDerivation?) {
            if (theme == null) operations.remove(step)
            else operations.set(step, ThemeForElement.Constant(theme))
        }
    }
}

private const val SEV_EXCEPTION = 2
private const val SEV_NOT_READY = 1
private const val SEV_OK = 0

@InternalKiteUi
public fun Element.ensureOutermostElement() {
    underlyingNativeElement.outermostElement = this
}