package com.lightningkite.kiteui.ssr

/**
 * Android implementation of HydrationContext.
 * No-op since Android doesn't use SSR hydration.
 */
actual object HydrationContext {
    actual var isHydrating: Boolean = false

    actual fun initFromDom() {
        // No-op on Android
    }

    actual fun getData(key: String): String? = null

    actual fun clear() {
        // No-op on Android
    }
}
