package com.lightningkite.kiteui.ssr

/**
 * Swing implementation of HydrationContext.
 * No-op since Swing doesn't use SSR hydration.
 */
actual object HydrationContext {
    actual fun initFromDom() {
        // No-op on Swing
    }

    actual fun getData(key: String): String? = null

    actual fun clear() {
        // No-op on Swing
    }

    actual var isHydrating: Boolean = false
}
