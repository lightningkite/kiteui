package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.printStackTrace2
import kotlin.random.Random

actual class RContext(
    val basePath: String,
    val dynamicCss: DynamicCss = DynamicCss(basePath),
    val kiteUiCss: KiteUiCss = KiteUiCss(dynamicCss),
) : RContextHelper() {
    val id = Random.nextInt()
    actual fun split(): RContext = RContext(basePath, dynamicCss, kiteUiCss).apply { addons.putAll(this@RContext.addons) }
    actual override val darkMode: Boolean? get() = null
    override fun toString(): String = "RContext@$id"
}