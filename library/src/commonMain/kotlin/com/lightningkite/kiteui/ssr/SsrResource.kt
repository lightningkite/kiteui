package com.lightningkite.kiteui.ssr

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Interface for SSR resource registration.
 * Implemented by SsrContext in jvmSsrMain.
 */
public interface SsrResourceRegistry {
    public fun registerResource(resource: SsrResource<*>)
}

private const val SSR_REGISTRY_KEY = "ssrResourceRegistry"

/**
 * Get the SSR resource registry from the context, if available.
 * Only non-null when rendering on the server side.
 */
public var ElementContext.ssrResourceRegistry: SsrResourceRegistry?
    get() = addons[SSR_REGISTRY_KEY] as? SsrResourceRegistry
    set(value) { addons[SSR_REGISTRY_KEY] = value }

/**
 * Create an SSR-aware resource that:
 * - During SSR: Registers with SsrContext for data loading and serialization
 * - During hydration: Loads from pre-rendered __SSR_DATA__ if available
 * - On client navigation: Fetches fresh data via the loader
 *
 * @param key Unique identifier for this resource. Must be unique per page.
 * @param loader Suspending function that fetches the data.
 * @return An SsrResource that can be used reactively in your UI.
 */
public inline fun <reified T : Any> ViewWriter.ssrResource(
    key: String,
    noinline loader: suspend () -> T
): SsrResource<T> {
    val resource = SsrResource(
        key = key,
        serializer = serializer<T>(),
        loader = loader
    )

    // Register with SSR context if available (server-side rendering)
    context.ssrResourceRegistry?.registerResource(resource)

    // Check for hydration data (client-side after SSR)
    HydrationContext.getData(key)?.let {
        resource.hydrateFrom(it)
    }

    // If not hydrated and no SSR registration, start loading on client
    if (!resource.isLoaded && context.ssrResourceRegistry == null) {
        resource.startLoading(this)
    }

    return resource
}

/**
 * A reactive resource for SSR data loading that:
 * 1. Integrates with KiteUI's reactive system as [Reactive<ReactiveState<T>>]
 * 2. Automatically serializes data into HTML for hydration
 * 3. Auto-detects completion (no explicit preload() function needed)
 * 4. Returns [ReactiveState] so you can handle loading/success/error states
 *
 * Usage in reactive contexts:
 * ```kotlin
 * val user = ssrResource("user") { fetchUser() }
 *
 * text {
 *     ::content {
 *         user().handle(
 *             success = { "Name: ${it.name}" },
 *             notReady = { "Loading..." },
 *             exception = { "Error: ${it.message}" }
 *         )
 *     }
 * }
 * ```
 *
 * @param T The type of data this resource holds. Must be @Serializable.
 * @param key Unique identifier for this resource. Used for serialization/hydration.
 * @param serializer The Kotlin serializer for type T.
 * @param loader Suspending function that fetches the data.
 */
public class SsrResource<T : Any>(
    public val key: String,
    private val serializer: KSerializer<T>,
    private val loader: suspend () -> T
) : Reactive<ReactiveState<T>> {

    private val _state = Signal<ReactiveState<T>>(ReactiveState.notReady)
    private var loadJob: Job? = null

    /**
     * The current state of this resource.
     * In reactive contexts, use the invoke operator `resource()` for proper dependency tracking.
     */
    override val state: ReactiveState<ReactiveState<T>>
        get() = ReactiveState(_state.state.getOrNull() ?: ReactiveState.notReady)

    /**
     * Get the inner ReactiveState directly (for non-reactive contexts).
     */
    public val innerState: ReactiveState<T>
        get() = _state.state.getOrNull() ?: ReactiveState.notReady

    override fun addListener(listener: () -> Unit): () -> Unit = _state.addListener(listener)

    public val isLoaded: Boolean get() = innerState.ready

    /**
     * Start loading (non-blocking).
     * Called automatically during SSR registration.
     */
    public fun startLoading(scope: CoroutineScope) {
        if (loadJob != null) return
        loadJob = scope.launch {
            try {
                val result = loader()
                _state.value = ReactiveState(result)
            } catch (e: Exception) {
                _state.value = ReactiveState.exception<T>(e)
            }
        }
    }

    /**
     * Initialize from serialized SSR data (hydration).
     * Called on client-side when __SSR_DATA__ is available.
     */
    public fun hydrateFrom(json: String) {
        val value = Json.decodeFromString(serializer, json)
        _state.value = ReactiveState(value)
    }

    /**
     * Suspend until the resource is loaded (success or exception).
     * Throws if the resource loading failed with an exception.
     */
    public suspend fun awaitLoaded(): T {
        return suspendCancellableCoroutine { cont ->
            var resolved = false
            var removeListener: (() -> Unit)? = null

            fun checkAndResolve() {
                if (resolved) return
                val currentState = innerState
                currentState.getOrNull()?.let {
                    resolved = true
                    removeListener?.invoke()
                    cont.resume(it)
                    return
                }
                currentState.exception?.let {
                    resolved = true
                    removeListener?.invoke()
                    cont.resumeWithException(it)
                    return
                }
            }

            // Add listener first
            removeListener = addListener { checkAndResolve() }

            // Then check immediately (handles case where resource completed before listener was added)
            checkAndResolve()

            cont.invokeOnCancellation { removeListener?.invoke() }
        }
    }

    /**
     * Serialize current value. Throws if not loaded.
     * Called during SSR to export data for client hydration.
     */
    public fun serialize(): String {
        val value = innerState.getOrNull()
            ?: throw IllegalStateException("Cannot serialize unloaded resource '$key'")
        return Json.encodeToString(serializer, value)
    }
}
