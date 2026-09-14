package com.lightningkite.kiteui.ssr

/**
 * JVM SSR implementation of HydrationContext.
 * No-op since server-side rendering doesn't need to hydrate from DOM.
 */
public actual object HydrationContext {
    public actual var isHydrating: Boolean = false

    public actual fun initFromDom() {
        // No-op on server side
    }

    public actual fun getData(key: String): String? = null

    public actual fun clear() {
        // No-op on server side
    }
}
