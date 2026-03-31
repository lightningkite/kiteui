@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.reactive.context.StatusListener
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

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

    override var parent: ContainerElement? = null
        internal set


    // ---- LIFECYCLE ---

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext = coroutineContextOf(
        job,
        CoroutineExceptionHandler { _, thr ->
            if (thr !is CancellationException) thr.report(this.toString())
        },
        context.ssrDispatcher ?: Dispatchers.Main.immediate,
        this as StatusListener
    )

    var fullyStarted = false
        private set

    override fun onStartup() {
        if (fullyStarted) return
        fullyStarted = true
        refreshTheming()
    }

    var isShutdown = false
        private set

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
        return (paddingByEdge ?: theme.padding).let { p ->
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
            val fromParent = GetBaseTheme { element -> element.parent?.theme?.let { it.revert ?: it } ?: Theme.placeholder }
            val fromParentNonCascading = GetBaseTheme { element -> element.parent?.theme ?: Theme.placeholder }
        }
    }

    /**
     * Strategy for applying state-based theme modifications (loading, error, working states).
     * Can be combined using the plus operator.
     */
    fun interface ElementSpecificTheming {
        operator fun invoke(element: NativeElement): ThemeDerivation

        operator fun plus(other: ElementSpecificTheming): ElementSpecificTheming {
            return ElementSpecificTheming { e -> this(e) + other(e) }
        }

        operator fun plus(theme: ThemeDerivation): ElementSpecificTheming {
            return ElementSpecificTheming { e -> this(e) + theme }
        }

        companion object {
            /** Default state theming that applies loading/working/error semantics */
            val loadingAndProcessing = ElementSpecificTheming { e ->
                val t = e.foregroundProcesses.state.handle(
                    success = { ThemeDerivation.None },
                    notReady = { WorkingSemantic },
                    exception = { WorkingSemantic }
                )

                if (!e.backgroundProcesses.state.success) t + LoadingSemantic else t
            }
        }
    }

    @ExperimentalKiteUi
    var themeBase: GetBaseTheme = GetBaseTheme.fromParent
        set(value) {
            field = value
            refreshTheming()
        }

    final override var themeChoice: ThemeDerivation = ThemeDerivation.None
        set(value) {
            field = value
            refreshTheming()
        }

    @ExperimentalKiteUi
    var elementSpecificTheming: ElementSpecificTheming = ElementSpecificTheming.loadingAndProcessing
        set(value) {
            field = value
            refreshTheming()
        }

    /**
     * Recalculates and applies the element's theme.
     *
     * Combines the base theme (from parent), theme choice (semantic modifiers like 'important'),
     * and state-based theming (loading/error states) to produce the final theme.
     */
    fun refreshTheming() {
        debug { "refreshTheming" }
        if (!checkActive("refreshTheming")) return
        if (parent?.underlyingNativeElement?.checkActive("refreshTheming.parent") == false) return
        val base = themeBase.get(this as NativeElement)
        debug {
            val source = when (themeBase) {
                GetBaseTheme.fromParent -> ""
                GetBaseTheme.fromParentNonCascading -> "Pulling non-cascading theme from parent."
                else -> "Pulling base theme from non-standard method."
            }
            "refreshTheming will set! $source Base theme is ${base.id}"
        }
        val t = themeChoice(base) + elementSpecificTheming(this)
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
                    releaseExceptionHandler?.invoke()
                    releaseExceptionHandler = null
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

                notReadyCount > 0 -> ReactiveState.notReady

                else -> ReactiveState(Unit)
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
        debugName ?: (theme.id + ' ' + this::class.toString().removePrefix("class ") + "@" + this.identityHashCode().toString(16))

    @InternalKiteUi
    open fun leakDetect() {
        WeakReference(this).checkLeakAfterDelay(1000)
    }

    @InternalKiteUi
    inline fun debug(requireTarget: Boolean = true, text: () -> String) {
        if ((!requireTarget && debugMode) || Element.Debugger.debugTarget === this) Log.tag("$this DEBUG").info(text())
    }

    fun currentlyActive(): Boolean = fullyStarted && !isShutdown

    @InternalKiteUi
    fun checkActive(name: String, requireTarget: Boolean = true): Boolean {
        if (isShutdown) {
            println("WARNING!! $this is shut down, but attempt to call $name was made")
            return false
        }
        if (!fullyStarted) {
            debug(requireTarget) { "$name abandoned due to not fully started" }
            return false
        }
        return true
    }
}

private const val SEV_EXCEPTION = 2
private const val SEV_NOT_READY = 1
private const val SEV_OK = 0
