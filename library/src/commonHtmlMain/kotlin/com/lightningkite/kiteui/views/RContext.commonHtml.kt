package com.lightningkite.kiteui.views

import kotlin.random.Random

actual class RContext(
    val basePath: String,
    val dynamicCss: DynamicCss = DynamicCss(basePath),
    val kiteUiCss: KiteUiCss = KiteUiCss(dynamicCss),
    parent: RContext? = null
) : RContextHelper(parent) {
    val id = Random.nextInt()

    actual fun split(): RContext = RContext(basePath, dynamicCss, kiteUiCss, this)

    actual override val darkMode: Boolean? get() = null
    override fun toString(): String = "RContext@$id"
    actual var immersiveMode: Boolean = false
    actual companion object {}
}