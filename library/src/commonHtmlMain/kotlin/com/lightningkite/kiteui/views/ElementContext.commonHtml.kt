package com.lightningkite.kiteui.views

import kotlin.random.Random

public actual class ElementContext(
    public val basePath: String,
    public val dynamicCss: DynamicCss = DynamicCss(basePath),
    public val kiteUiCss: KiteUiCss = KiteUiCss(dynamicCss),
    parent: ElementContext? = null
) : ElementContextCommonCode(parent) {
    public val id = Random.nextInt()

    public actual fun split(): ElementContext = ElementContext(basePath, dynamicCss, kiteUiCss, this)

    public actual val darkMode: Boolean? get() = null

    override fun toString(): String = "RContext@$id"
    public actual var immersiveMode: Boolean = false
    public actual companion object {}
}