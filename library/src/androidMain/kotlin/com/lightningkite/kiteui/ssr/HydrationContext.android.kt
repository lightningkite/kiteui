package com.lightningkite.kiteui.ssr

/**
 * Android implementation of HydrationContext.
 * No-op since Android doesn't use SSR hydration.
 */
public actual object HydrationContext {
    public actual var isHydrating: Boolean = false

    public actual fun initFromDom() {
        // No-op on Android
    }

    public actual fun getData(key: String): String? = null

    public actual fun clear() {
        // No-op on Android
    }
}
