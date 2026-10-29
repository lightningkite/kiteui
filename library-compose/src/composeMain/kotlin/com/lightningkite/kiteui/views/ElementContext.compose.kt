package com.lightningkite.kiteui.views

public actual class ElementContext(parent: ElementContext? = null, shareAddons: Boolean = false) : ElementContextCommonCode(parent, shareAddons) {
    public actual fun split(): ElementContext = ElementContext(this)
    public actual fun splitExceptionHandlers(): ElementContext = ElementContext(this, shareAddons = true)

    // TODO: follow the OS dark mode setting.
    public actual val darkMode: Boolean? get() = null
    public actual var immersiveMode: Boolean = false

    public actual companion object
}
