package com.lightningkite.kiteui.ssr

/**
 * Platform-specific context for SSR hydration.
 *
 * On JS: Parses __SSR_DATA__ from the DOM and provides cached data
 * On other platforms: No-op (returns null for all data)
 */
expect object HydrationContext {
    /**
     * Initialize hydration data from platform-specific source.
     * On JS: Parses __SSR_DATA__ script element from DOM.
     * On other platforms: No-op.
     */
    fun initFromDom()

    /**
     * Get serialized resource data by key.
     * @return JSON string if data was pre-loaded during SSR, null otherwise.
     */
    fun getData(key: String): String?

    /**
     * Clear cached hydration data.
     * Call after hydration is complete to free memory.
     */
    fun clear()

    /**
     * True when the app is in hydration mode (reusing server-rendered DOM).
     * Set to true by initFromDom() if SSR data is found, false by clear().
     */
    var isHydrating: Boolean
}
