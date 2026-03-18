@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.reactive.context.StatusListener
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

expect abstract class NativeElement(context: ElementContext) : NativeElementCommonCode {

}

sealed class NativeElementCommonCode(override val context: ElementContext) : Element {
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

    @InternalKiteUi
    open fun startup() {
        if (fullyStarted) return
        fullyStarted = true
        refreshTheming()
    }

    var isShutdown = false
        private set

    open fun shutdown() {
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

    val appliedPadding: Edges get() {
        if (!fullyStarted) println("WARN: $this attempted to calculate applied padding before fully started.")
        return (paddingByEdge ?: theme.padding).let { p ->
            safeAreaPadding?.let { p + it } ?: p
        }
    }

    // Theme pipeline

    fun interface GetBaseTheme {
        fun get(element: NativeElement): Theme

        companion object {
            val fromParent = GetBaseTheme { element -> element.parent?.theme?.let { it.revert ?: it } ?: Theme.placeholder }
            val fromParentNonCascading = GetBaseTheme { element -> element.parent?.theme ?: Theme.placeholder }
        }
    }

    fun interface StateTheming {    // this will work for now... But in the future I think a better approach would have key-theme pairs that code can add/remove from.
        operator fun invoke(element: NativeElement): ThemeDerivation

        operator fun plus(other: StateTheming): StateTheming {
            return StateTheming { e -> this(e) + other(e) }
        }

        companion object {
            val loadingAndProcessing = StateTheming { e ->
                val t = e.foregroundProcesses.state.handle(   // apply working semantics for foreground processes (like button presses)
                    success = { ThemeDerivation.None },
                    notReady = { WorkingSemantic },
                    exception = { WorkingSemantic + ErrorSemantic }
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
    var appliedStatefulTheming: StateTheming = StateTheming.loadingAndProcessing
        set(value) {
            field = value
            refreshTheming()
        }

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
        val t = themeChoice(base) + appliedStatefulTheming(this)
        debug { "refreshTheming will set to ${t.theme.id}!" }
        themeAndBack = t
    }


    // --- PROCESSING ---

    private inner class Processes : BaseListenable(), Reactive<Unit> {
        private val processes = HashSet<Reactive<*>>()

        private var exceptionCount = 0
        private var notReadyCount = 0

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
                    val firstException = processes.firstNotNullOfOrNull { it.state.exception }
                    if (firstException == null) {
                        exceptionCount = 0 // recurse with new (accurate) exception count and then return because we already calculated it
                        recalculateState()
                        return
                    } else ReactiveState.exception(firstException)
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

            var prevSeverity: Int? = null

            val release = status.addAndRunListener {
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

    private val internalBackgroundProcesses = Processes()
    private val internalForegroundProcesses = Processes()

    override fun watchBackgroundProcess(status: Reactive<*>): Release = internalBackgroundProcesses.watch(status).also(::onRemove)
    override fun watchForegroundProcess(status: Reactive<*>): Release = internalForegroundProcesses.watch(status).also(::onRemove)

    val backgroundProcesses: Reactive<*> get() = internalBackgroundProcesses
    val foregroundProcesses: Reactive<*> get() = internalForegroundProcesses


    // --- DEBUGGING & DRIVER ---

    override var debugName: String? = null

    override fun toString(): String =
        debugName ?: (theme.id + ' ' + this::class.toString().removePrefix("class ") + "@" + this.identityHashCode().toString(16))

    @InternalKiteUi
    open fun leakDetect() {
        WeakReference(this).checkLeakAfterDelay(1000)
    }

    @PublishedApi
    internal inline fun debug(text: () -> String) {
        if (this === Element.Debugger.debugTarget) println("$this DEBUG: ${text()}")
    }

    fun currentlyActive(): Boolean = fullyStarted && !isShutdown

    @InternalKiteUi
    fun checkActive(name: String): Boolean {
        if (isShutdown) {
            println("WARNING!! $this is shut down, but attempt to call $name was made")
            return false
        }
        if (!fullyStarted) {
            debug { "$name abandoned due to not fully started" }
            return false
        }
        return true
    }
}

private const val SEV_EXCEPTION = 2
private const val SEV_NOT_READY = 1
private const val SEV_OK = 0
