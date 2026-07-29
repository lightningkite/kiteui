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
    private var hydratedElements: Int = 0
    private var createdElements: Int = 0
    private var mismatchedElements: Int = 0
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
        hydratedElements = 0
        createdElements = 0
        mismatchedElements = 0
        hydrationTimeMs = 0.0
        hydrationStartTimeMs = 0.0
    }
}
