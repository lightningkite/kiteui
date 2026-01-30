package com.lightningkite.kiteui.ssr

/**
 * iOS implementation of HydrationContext.
 * No-op since iOS doesn't use SSR hydration.
 */
actual object HydrationContext {
    actual var isHydrating: Boolean = false

    actual fun initFromDom() {
        // No-op on iOS
    }

    actual fun getData(key: String): String? = null

    actual fun clear() {
        // No-op on iOS
    }
}
