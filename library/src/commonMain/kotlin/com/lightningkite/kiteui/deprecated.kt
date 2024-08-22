package com.lightningkite.kiteui

operator fun Nothing.minus(other: Nothing): Nothing = TODO()
operator fun Nothing.contains(other: Nothing): Boolean = TODO()

@Deprecated("Use kotlinx.coroutines instead") val delay = Unit