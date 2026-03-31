package com.lightningkite.kiteui.views

import kotlin.random.Random

actual class ElementContext(
    val basePath: String,
    val dynamicCss: DynamicCss = DynamicCss(basePath),
    val kiteUiCss: KiteUiCss = KiteUiCss(dynamicCss),
    parent: ElementContext? = null
) : ElementContextCommonCode(parent) {
    val id = Random.nextInt()

    actual fun split(): ElementContext = ElementContext(basePath, dynamicCss, kiteUiCss, this)

    actual val darkMode: Boolean? get() = null

    override fun toString(): String = "RContext@$id"
    actual var immersiveMode: Boolean = false
    actual companion object {}
}