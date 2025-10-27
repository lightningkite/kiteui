package com.lightningkite.kiteui.views

import kotlin.random.Random

actual class RContext(
) : RContextHelper() {
    val id = Random.nextInt()
    actual fun split(): RContext = RContext().apply { addons.putAll(this@RContext.addons) }
    actual override val darkMode: Boolean? get() = null
    override fun toString(): String = "RContext@$id"
    actual var immersiveMode: Boolean = false
}