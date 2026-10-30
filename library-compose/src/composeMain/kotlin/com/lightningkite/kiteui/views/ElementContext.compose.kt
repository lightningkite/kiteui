package com.lightningkite.kiteui.views

public actual class ElementContext(parent: ElementContext? = null) : ElementContextCommonCode(parent) {
    public actual fun split(): ElementContext = ElementContext(this)

    // TODO: follow the OS dark mode setting.
    public actual val darkMode: Boolean? get() = null
    public actual var immersiveMode: Boolean = false

    public actual val root: ElementContext = parent?.root ?: this

    public actual companion object : kotlin.coroutines.CoroutineContext.Key<ElementContext>
}
