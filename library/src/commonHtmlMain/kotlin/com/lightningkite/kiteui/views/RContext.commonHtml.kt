package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.printStackTrace2
import kotlin.random.Random

public actual class RContext(
    public val basePath: String,
    public val dynamicCss: DynamicCss = DynamicCss(basePath),
    public val kiteUiCss: KiteUiCss = KiteUiCss(dynamicCss),
) : RContextHelper() {
    public val id: Int = Random.nextInt()
    public actual fun split(): RContext = RContext(basePath, dynamicCss, kiteUiCss).apply { addons.putAll(this@RContext.addons) }
    public actual override val darkMode: Boolean? get() = null
    public override fun toString(): String = "RContext@$id"
}