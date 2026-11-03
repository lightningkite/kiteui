package com.lightningkite.kiteui.ssr

/**
 * iOS implementation of HydrationContext.
 * No-op since iOS doesn't use SSR hydration.
 */
public actual object HydrationContext {
    public actual var isHydrating: Boolean = false

    public actual fun initFromDom() {
        // No-op on iOS
    }

    public actual fun getData(key: String): String? = null

    public actual fun clear() {
        // No-op on iOS
    }
}
