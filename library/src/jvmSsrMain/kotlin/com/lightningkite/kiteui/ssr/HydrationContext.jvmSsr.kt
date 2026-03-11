package com.lightningkite.kiteui.ssr

/**
 * JVM SSR implementation of HydrationContext.
 * No-op since server-side rendering doesn't need to hydrate from DOM.
 */
actual object HydrationContext {
    actual var isHydrating: Boolean = false

    actual fun initFromDom() {
        // No-op on server side
    }

    actual fun getData(key: String): String? = null

    actual fun clear() {
        // No-op on server side
    }
}
