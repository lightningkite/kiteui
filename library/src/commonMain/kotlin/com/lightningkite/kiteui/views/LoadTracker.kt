@file:MustUseReturnValues

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.state
import com.lightningkite.reactive.core.BaseReactive
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.lensing.lens
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

/**
 * Tracks whether any element it covers is loading the data it shows, as a
 * [Reactive][com.lightningkite.reactive.core.Reactive] that is not ready while one is.
 *
 * Every element reports to its context's [loadTracker]: the one of the nearest element made [withLoadTracking], or
 * the element tree's root tracker if there is none. A tracker counts as loading for the one outside it too, so the
 * root tracker covers the whole tree:
 * ```kotlin
 * val section = withLoadTracking().col { /* content */ }
 * shownWhen { section.context.loadTracker.loading() }.activityIndicator()
 * ```
 *
 * Reporting costs the same however deeply trackers are nested: a tracker tells the one outside it only when it starts
 * or stops loading, not about each element it covers.
 */
public class LoadTracker internal constructor(
    private val outside: LoadTracker?,
    loadCounter: MutableReactiveValue<Int> = Signal(0)
) : BaseReactive<Unit>(ReactiveState(Unit)) {
    init {
        loadCounter.value = 0
    }

    private var load by loadCounter

    /** Records that something this covers started loading; call [finishedLoading] once it has. */
    internal fun startedLoading() {
        if (load++ == 0) {
            state = ReactiveState.notReady
        }
        outside?.startedLoading()
    }

    /** Records that something [startedLoading] has finished. */
    internal fun finishedLoading() {
        if (--load == 0) {
            state = ReactiveState(Unit)
        }
        outside?.finishedLoading()
    }

    public val loadCount: ReactiveValue<Int> = loadCounter
    public val loading: ReactiveValue<Boolean> = loadCounter.lens { it > 0 }
}

private var ElementContext.nearestLoadTracker: LoadTracker? by contextAddon(null)
private val ElementContext.rootLoadTracker: LoadTracker by lazyContextAddon(ContextAddon.Scope.Global) { LoadTracker(null) }

/** The tracker elements in this context report their loading to; see [LoadTracker]. */
public val ElementContext.loadTracker: LoadTracker get() = nearestLoadTracker ?: rootLoadTracker

private class WithLoadTracking(parent: ElementWriter, loadCounter: MutableReactiveValue<Int> = Signal(0)) : ViewWriter, ElementWriter by parent {
    override val context: ElementContext = parent.context.split().also {
        it.nearestLoadTracker = LoadTracker(outside = it.loadTracker, loadCounter)
    }
}

/** Returns a writer whose element gets its own [LoadTracker], covering that element and everything inside it. */
public fun ElementWriter.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ElementWriter = WithLoadTracking(this, reportTo)
public fun ElementWriter.CanAddScrolling.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ElementWriter.CanAddScrolling = WithLoadTracking(this, reportTo)
public fun ElementWriter.CanAddTheme.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ElementWriter.CanAddTheme = WithLoadTracking(this, reportTo)
public fun ElementWriter.CanAddSizing.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ElementWriter.CanAddSizing = WithLoadTracking(this, reportTo)
public fun ElementWriter.CanAddShownWhen.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ElementWriter.CanAddShownWhen = WithLoadTracking(this, reportTo)
public fun ElementWriter.CanAddWeight.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ElementWriter.CanAddWeight = WithLoadTracking(this, reportTo)
public fun ElementWriter.CanAddAlignment.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ElementWriter.CanAddAlignment = WithLoadTracking(this, reportTo)
public fun ViewWriter.withLoadTracking(reportTo: MutableReactiveValue<Int> = Signal(0)): ViewWriter = WithLoadTracking(this, reportTo)

@IgnorableReturnValue
public fun <T> ElementWriter.withLoadingProgress(render: ElementWriter.(Reactive<Float?>) -> T): T {
    val count = Signal(0)
    var max = 0
    var prev: Float? = null

    val countDb = count.debounce(50.milliseconds)

    val progress = remember(reentrancyLimit = 10) {
        val c = countDb()

        if (c <= 0) {
            prev = null
            max = 0
            return@remember null
        }

        val new: Float = if (c > max) {
            max = c
            // asymptotically approach 5% while load count is increasing
            0.05f * asymp(c / 8f)
        } else {
            // count has decreased, new progress is percent completed over the max count
            1f - (c / max.toFloat())
        }.coerceIn((prev ?: 0f)..1f) // never decrease in progress

        prev = new

        new
    }

    return withLoadTracking(count).render(progress)
}

private fun asymp(x: Float): Float = x / sqrt(x*x + 1)