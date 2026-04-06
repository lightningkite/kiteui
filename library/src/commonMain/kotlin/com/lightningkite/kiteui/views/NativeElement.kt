@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.telemetry.TelemetryContext
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
 * This is the platform-native view wrapper that provides actual rendering on each platform
 * (Android View, iOS UIView, HTML Element, etc.). Each platform provides its own implementation.
 */
expect abstract class NativeElement(context: ElementContext) : Element, NativeElementCommonCode {
    override var opacity: Double

    override var shown: Boolean

    override var visible: Boolean

    override var ignoreInteraction: Boolean

    override var dragData: DragData?

    override var dropTargetDelegate: DropTargetDelegate?

    override fun nativeApplyTheme(theme: ThemeAndBack)

    override fun refreshPadding()

    override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean)

    override fun requestFocus()

    /** Returns the screen-relative rectangle occupied by this element */
    fun screenRectangle(): Rect?

    /** Returns the parent-relative rectangle occupied by this element */
    fun parentRectangle(): Rect?

    override var showOnPrint: Boolean
}

/**
 * Shared platform-independent code for all native elements.
 *
 * This class provides common implementation for lifecycle, theming, reactive processes, and debugging
 * that is shared across all platforms. Platform-specific implementations extend this through [NativeElement].
 *
 * You should never extend this directly. If you want to create a custom native component inherit from [NativeElement].
 */
abstract class NativeElementCommonCode internal constructor(override val context: ElementContext) : Element {
    // This code is duplicated in every element throughout the entire view tree, so performance actually kinda matters.

    override val underlyingNativeElement: NativeElement get() = this as NativeElement

    /**
     * Elements are designed for delegation, so you can create a wrapper element by doing:
     *
     * ```
     * class Wrapper(inner: NativeElement) : Element by inner { ... }
     * ```
     *
     * However, this delegation can lead to problems, as the internal native logic has no way to
     * access the wrapper element. This variable is the solution - it provides a reference to the outermost
     * wrapper element over the native component. This value is set when written to the view tree in
     * [ElementWriter.write].
     * */
    internal open var outermostElement: Element = this

    override var parent: ContainerElement? = null
        internal set(value) {
            debug { "setting parent to $value" }
            field = value
            if (value != null) refreshTheming()
        }

    // ---- LIFECYCLE ---

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
        this as StatusListener,
        TelemetryContext(element = this as NativeElement)
    )

    var fullyStarted = false
        private set

    @OptIn(OverrideOnly::class)
    override fun onStartup() {
        if (fullyStarted) return
        fullyStarted = true
        debug { "starting..." }
        refreshTheming()
        debug { "new theme: ${theme.id}" }
    }

    var isShutdown = false
        private set

    @OptIn(OverrideOnly::class)
    override fun onShutdown() {
        if (isShutdown) return
        job.cancel()
        isShutdown = true
        if (Element.Debugger.leakDetect) leakDetect()
        parent = null
    }


    // --- THEMING ---

    protected abstract fun nativeApplyTheme(theme: ThemeAndBack)
    abstract fun refreshPadding()

    override var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
        protected set(value) {
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
    val appliedPadding: Edges get() {
        if (!fullyStarted) println("WARN: $this attempted to calculate applied padding before fully started.")
        return (paddingByEdge ?: theme.padding.takeIf { themeAndBack.padding } ?: Edges.ZERO).let { p ->
            safeAreaPadding?.let { p + it } ?: p
        }
    }

    // Theme pipeline

    /**
     * Strategy for determining the base theme of an element.
     * Default strategy inherits cascading theme from parent.
     */
    fun interface GetBaseTheme {
        fun get(element: NativeElement): Theme

        companion object {
            val fromParent = GetBaseTheme { element ->
                element.parent?.theme?.let { it.revert ?: it } ?: Theme.placeholder
            }
            val fromParentNonCascading = GetBaseTheme { element -> element.parent?.theme ?: Theme.placeholder }
        }
    }

    @ExperimentalKiteUi
    var themeBase: GetBaseTheme = GetBaseTheme.fromParent
        set(value) {
            field = value
            refreshTheming()
        }

    @ExperimentalKiteUi
    var themePipeline: ThemePipeline = ThemePipeline(
        ThemePipeline.Step.userChoice to null,
        ThemePipeline.Step.processingStatus to ThemePipeline.Operation.processingTheming
    )

    final override var themeChoice: ThemeDerivation
        get() = themePipeline.get(ThemePipeline.Step.userChoice, outermostElement)
        set(value) {
            themePipeline.set(ThemePipeline.Step.userChoice, value)
            refreshTheming()
        }

    /**
     * Recalculates and applies the element's theme.
     *
     * Combines the base theme (typically from parent), theme choice (semantic modifiers like 'important'),
     * and state-based theming (loading/error states) to produce the final theme.
     */
    fun refreshTheming() {
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

    override fun watchBackgroundProcess(status: Reactive<*>): Release = internalBackgroundProcesses.watch(status).also(::onRemove)
    override fun watchForegroundProcess(status: Reactive<*>): Release = internalForegroundProcesses.watch(status).also(::onRemove)

    /** Aggregate state of background processes (data loading, etc.) - affects loading semantics */
    val backgroundProcesses: Reactive<*> get() = internalBackgroundProcesses

    /** Aggregate state of foreground processes (button clicks, etc.) - affects working/error semantics */
    val foregroundProcesses: Reactive<*> get() = internalForegroundProcesses


    // --- DEBUGGING & DRIVER ---

    override var debugName: String? = null

    override fun toString(): String =
        outermostElement.debugName ?: (theme.id + ' ' + outermostElement::class.toString().removePrefix("class ") + "&" + outermostElement.identityHashCode().toString(16))

    @InternalKiteUi
    open fun leakDetect() {
        WeakReference(this).checkLeakAfterDelay(1000)
    }

    @InternalKiteUi
    inline fun debug(requireTarget: Boolean = true, text: () -> String) {
        if ((!requireTarget && debugMode) || Element.Debugger.debugTarget?.underlyingNativeElement === this) Log.tag("$this DEBUG").info(text())
    }

    fun currentlyActive(): Boolean = fullyStarted && !isShutdown

    @InternalKiteUi
    fun checkIsShutdown(name: String): Boolean {
        if (isShutdown) {
            println("WARNING!! $this is shut down, but attempt to call $name was made")
            return true
        }
        return false
    }

    @InternalKiteUi
    fun checkIsActive(name: String, requireTarget: Boolean = true): Boolean {
        if (checkIsShutdown(name)) return false
        if (!fullyStarted) {
            debug(requireTarget) { "$name abandoned due to not fully started" }
            return false
        }
        return true
    }

    class ThemePipeline(private val operations: ArrayList<Pair<Step, Operation?>>) {
        constructor(vararg init: Pair<Step, Operation?>) : this(arrayListOf(*init))

        @JvmInline
        value class Step(val order: Float) {
            companion object {
                val elementStyling = Step(0f)       // 1. what the element chooses for itself
                val userChoice = Step(0.2f)         // 2. what the user chooses for the element
                val dynamicChoice = Step(0.4f)      // 3. what the user chooses for the element but reactive (dynamicThemed)
                val elementStatus = Step(0.6f)      // 4. themes applied to the element because of its own internal state (selected, checked, disabled, etc.)
                val processingStatus = Step(0.8f)   // 5. theming applied because of loading/processing of background/foreground tasks in the element
            }
        }

        sealed interface Operation {
            fun get(element: Element): ThemeDerivation
            fun apply(element: Element, theme: Theme): ThemeAndBack = get(element)(theme)

            operator fun plus(other: Operation): Operation = Chain(this, other)

            data class Constant(val theme: ThemeDerivation) : Operation {
                override fun get(element: Element): ThemeDerivation = theme
            }
            data class Variable(val theme: (Element) -> ThemeDerivation) : Operation {
                override fun get(element: Element): ThemeDerivation = theme(element)
            }
            data class Chain(val left: Operation, val right: Operation): Operation {
                override fun get(element: Element): ThemeDerivation =
                    ThemeDerivation.Chain(left.get(element), right.get(element))

                override fun apply(element: Element, theme: Theme): ThemeAndBack =
                    left.apply(element, theme) + right.get(element)
            }

            companion object {
                val processingTheming = Variable { element ->
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

        fun get(step: Step, element: Element): ThemeDerivation =
            operations.find { it.first == step }?.second?.get(element) ?: ThemeDerivation.None

        fun get(element: Element): ThemeDerivation =
            operations.fold(null) { acc: ThemeDerivation?, (_, op) ->
                when {
                    acc == null -> op?.get(element)
                    op == null -> acc
                    else -> acc + op.get(element)
                }
            } ?: ThemeDerivation.None

        fun apply(element: Element, theme: Theme): ThemeAndBack =
            operations.fold(null) { acc: ThemeAndBack?, (_, op) ->
                when {
                    acc == null -> op?.apply(element, theme)
                    op == null -> acc
                    else -> acc + op.get(element)
                }
            } ?: theme.withoutBack

        fun add(step: Step, op: Operation) {
            val idx = operations.indexOfFirst { it.first == step }
            if (idx == -1) {
                operations.add(step to op)
                operations.sortBy { it.first.order }
            }
            else {
                val c = operations[idx].second
                operations[idx] = step to (c?.plus(op) ?: op)
            }
        }

        fun add(step: Step, theme: ThemeDerivation) = add(step, Operation.Constant(theme))
        fun add(step: Step, theme: (Element) -> ThemeDerivation) = add(step, Operation.Variable(theme))

        fun set(step: Step, op: Operation?) {
            val idx = operations.indexOfFirst { it.first == step }
            if (idx == -1) {
                operations.add(step to op)
                operations.sortBy { it.first.order }
            }
            else {
                operations[idx] = step to op
            }
        }

        fun set(step: Step, theme: ThemeDerivation?) = set(step, theme?.let(Operation::Constant))
        fun set(step: Step, theme: (Element) -> ThemeDerivation) = set(step, Operation.Variable(theme))
    }
}

private const val SEV_EXCEPTION = 2
private const val SEV_NOT_READY = 1
private const val SEV_OK = 0

@InternalKiteUi
fun Element.ensureOutermostElement() {
    underlyingNativeElement.outermostElement = this
}