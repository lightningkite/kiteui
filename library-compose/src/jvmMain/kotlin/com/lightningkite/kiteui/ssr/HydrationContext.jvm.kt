package com.lightningkite.kiteui.ssr

// Desktop apps are never server-rendered, so there is nothing to hydrate.
public actual object HydrationContext {
    public actual var isHydrating: Boolean = false
    public actual fun initFromDom() {}
    public actual fun getData(key: String): String? = null
    public actual fun clear() {}
}
