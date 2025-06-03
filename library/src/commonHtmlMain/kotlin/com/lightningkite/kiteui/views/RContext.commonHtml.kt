package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.readable.Property
import com.lightningkite.readable.Readable
import kotlin.random.Random

actual class RContext(
    val basePath: String,
    val dynamicCss: DynamicCss = DynamicCss(basePath),
    val kiteUiCss: KiteUiCss = KiteUiCss(dynamicCss),
) : RContextHelper() {
    val id = Random.nextInt()
    actual fun split(): RContext = RContext(basePath, dynamicCss, kiteUiCss).apply { _safeInsets = this@RContext._safeInsets; addons.putAll(this@RContext.addons) }
    actual override val darkMode: Boolean? get() = null
    override fun toString(): String = "RContext@$id"
    actual var immersiveMode: Boolean = false
    private var _safeInsets = Property(Edges.ZERO)
    actual val safeInsets: Readable<Edges> get() = _safeInsets
    fun testSafeInsets(edge: Edges) { _safeInsets.value = edge }
}