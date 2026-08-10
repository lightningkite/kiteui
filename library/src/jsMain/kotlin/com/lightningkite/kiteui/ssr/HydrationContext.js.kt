package com.lightningkite.kiteui.ssr

import kotlinx.browser.document
import kotlinx.serialization.json.Json

/**
 * JS implementation of HydrationContext.
 * Parses __SSR_DATA__ script element from the DOM for client-side hydration.
 *
 * Updated by Claude to add hydration statistics.
 */
public actual object HydrationContext {
    private var data: Map<String, String>? = null

    /**
     * True when the app is in hydration mode (reusing server-rendered DOM).
     * Set to true by initFromDom() if SSR data is found, false by clear().
     */
    public actual var isHydrating: Boolean = false

    // Hydration statistics - by Claude
    /**
     * Count of elements successfully hydrated onto existing SSR-rendered DOM during the current
     * (or most recently completed) hydration pass. Mirrors the [com.lightningkite.kiteui.views.Element.Debugger]
     * opt-in-counter idiom: a cheap, always-on counter exposed read-only so dev tooling can watch it
     * without re-deriving it from console output.
     */
    public var hydratedElements: Int = 0
        private set

    /** Count of elements that could not be hydrated and were created fresh instead. */
    public var createdElements: Int = 0
        private set

    /**
     * Count of tag mismatches between the SSR-rendered DOM and the client's expected structure.
     * A nonzero count during development means server and client disagree on DOM shape for at
     * least one element - see [recordMismatch]'s call site for the recovery behavior (the mismatched
     * subtree is replaced, not left broken); this counter exists purely to make the mismatch rate
     * visible instead of only appearing as scattered console warnings.
     */
    public var mismatchedElements: Int = 0
        private set
    private var hydrationTimeMs: Double = 0.0
    private var hydrationStartTimeMs: Double = 0.0

    /**
     * Initialize from __SSR_DATA__ script element in the DOM.
     * Call this on page load before rendering components.
     */
    public actual fun initFromDom() {
        // Reset stats - by Claude
        hydratedElements = 0
        createdElements = 0
        mismatchedElements = 0

        val script = document.getElementById("__SSR_DATA__")
        if (script != null) {
            val textContent = script.textContent
            if (!textContent.isNullOrBlank()) {
                try {
                    data = Json.decodeFromString(textContent)
                    isHydrating = data != null
                    if (isHydrating) {
                        console.log("[KiteUI Hydration] Found ${data?.size ?: 0} SSR resource(s)")
                    }
                } catch (e: Exception) {
                    console.error("Failed to parse __SSR_DATA__:", e)
                    data = null
                    isHydrating = false
                }
            } else {
                // Empty SSR data means SSR was used but no resources were serialized
                isHydrating = true
                console.log("[KiteUI Hydration] SSR detected (no resource data)")
            }
        }
    }

    /**
     * Get serialized resource data by key.
     * @return JSON string if data was pre-loaded during SSR, null otherwise.
     */
    public actual fun getData(key: String): String? = data?.get(key)

    /**
     * Record a successful element hydration. by Claude
     */
    public fun recordHydrated() {
        hydratedElements++
    }

    /**
     * Record a new element creation (hydration not possible). by Claude
     */
    public fun recordCreated() {
        createdElements++
    }

    /**
     * Record a hydration mismatch. by Claude
     */
    public fun recordMismatch() {
        mismatchedElements++
    }

    /**
     * Record the time hydration took in milliseconds. by Claude
     */
    public fun recordHydrationTime(timeMs: Double) {
        hydrationTimeMs = timeMs
    }

    /**
     * Mark when hydration starts for internal timing. by Claude
     */
    public fun markHydrationStart() {
        hydrationStartTimeMs = kotlin.js.Date.now()
    }

    /**
     * Clear cached hydration data after hydration is complete.
     *
     * Deliberately leaves [hydratedElements], [createdElements], and [mismatchedElements] alone:
     * [initFromDom] resets them when the *next* page load begins hydrating, but for the lifetime of
     * the current page they stay readable (e.g. from the browser console) so a nonzero
     * [mismatchedElements] after hydration finishes is actually visible to a developer, not just a
     * number that flashed by in a console.log the instant before this reset it to 0.
     */
    public actual fun clear() {
        // Log detailed stats before clearing - by Claude
        val total = hydratedElements + createdElements
        if (total > 0) {
            val successRate = if (total > 0) (hydratedElements * 100.0 / total).toInt() else 100
            val timeStr = if (hydrationTimeMs > 0) " in ${hydrationTimeMs.toInt()}ms" else ""
            console.log("[KiteUI Hydration] Complete$timeStr: $hydratedElements/$total elements hydrated ($successRate%), $mismatchedElements mismatches, $createdElements created fresh")
        } else if (isHydrating) {
            console.log("[KiteUI Hydration] Complete (no elements processed)")
        }

        data = null
        isHydrating = false
        hydrationTimeMs = 0.0
        hydrationStartTimeMs = 0.0
    }

    /**
     * Resets the hydration statistics counters back to zero.
     *
     * Separate from [clear]: production code never needs this (the counters are reset by the next
     * [initFromDom] instead, so they stay inspectable in between), but tests that assert on specific
     * counter values need a way to force isolation between cases.
     */
    public fun resetStats() {
        hydratedElements = 0
        createdElements = 0
        mismatchedElements = 0
        hydrationTimeMs = 0.0
        hydrationStartTimeMs = 0.0
    }
}
