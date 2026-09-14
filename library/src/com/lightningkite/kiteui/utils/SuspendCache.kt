package com.lightningkite.kiteui.utils

import com.lightningkite.reactive.core.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Caches the result of a suspending computation per key.
 *
 * Handles the three things a hand-rolled `HashMap<K, Deferred<V>>` cache tends to get wrong:
 *
 * - **Duplicate work.** A caller arriving while a computation is in flight joins it instead of
 *   starting a second one.
 * - **Cached failures.** A computation that throws is dropped, so the next [get] tries again.
 *   Keeping the failed `Deferred` means every later call replays the same stale error forever.
 * - **Cancellation is not failure.** Eviction hangs off the computation's own completion rather than
 *   a `catch` around `await()`. [scope] outlives its callers, so a cancelled *caller* makes `await()`
 *   throw while the computation carries on; evicting there would drop a result that was about to
 *   arrive and orphan whatever it had already allocated.
 *
 * The map itself is confined to [scope]'s dispatcher rather than being synchronized: [get] hops onto
 * it before touching the map, so callers may arrive from any dispatcher. Only the dispatcher is
 * borrowed, not the whole context - inheriting [scope]'s Job would quietly reparent the caller.
 */
internal class SuspendCache<K, V>(
    private val scope: CoroutineScope = AppScope,
    private val compute: suspend (K) -> V,
) {
    private val entries = HashMap<K, Deferred<V>>()
    // Keyed on ContinuationInterceptor rather than CoroutineDispatcher: a dispatcher is one, and
    // that key is stable where the class-based CoroutineDispatcher lookup needs an opt-in.
    private val confinement: CoroutineContext = scope.coroutineContext[ContinuationInterceptor] ?: EmptyCoroutineContext

    /** The value for [key], starting the computation if no one has yet, or joining one in flight. */
    suspend fun get(key: K): V = withContext(confinement) { entryFor(key) }.await()

    /**
     * The computation for [key] if one has been started, without starting one.
     *
     * Unlike [get] this does not hop dispatchers, so call it from [scope]'s.
     */
    fun peek(key: K): Deferred<V>? = entries[key]

    /**
     * Forgets [key], so the next [get] recomputes.
     *
     * A computation already in flight keeps running and callers already awaiting it still get their
     * result; it just stops being handed to anyone new.
     *
     * Unlike [get] this does not hop dispatchers, so call it from [scope]'s.
     */
    fun forget(key: K) {
        entries.remove(key)
    }

    private fun entryFor(key: K): Deferred<V> {
        entries[key]?.let { return it }
        val started = scope.async { compute(key) }
        // Stored before the completion handler is attached, because `scope` may run the computation
        // inline - Dispatchers.Main.immediate does when already on main - so one that fails without
        // ever suspending is already complete by this line and the handler fires the moment it is
        // attached. Attaching first would have it look for an entry that is not in the map yet,
        // decline to evict, and leave the failure cached: exactly the bug this class exists to avoid.
        entries[key] = started
        started.invokeOnCompletion { cause ->
            // Identity-checked so a retry that already replaced the entry is left alone.
            if (cause != null && entries[key] === started) entries.remove(key)
        }
        return started
    }
}
