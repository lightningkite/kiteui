package com.lightningkite.kiteui

@Deprecated("Use dot syntax instead", level = DeprecationLevel.ERROR)
operator fun Nothing.minus(other: Nothing): Nothing = TODO()
@Deprecated("Use dot syntax instead", level = DeprecationLevel.ERROR)
operator fun Nothing.contains(other: Nothing): Boolean = TODO()

@Deprecated("Use kotlinx.coroutines instead") val delay = Unit

@Deprecated("import has moved", ReplaceWith("encodeURIComponent(content)", "com.lightningkite.kotlinx.serialization.uri.encodeURIComponent"))
fun encodeURIComponent(content: String) = com.lightningkite.kotlinx.serialization.uri.encodeURIComponent(content)

@Deprecated("import has moved", ReplaceWith("decodeURIComponent(content)", "com.lightningkite.kotlinx.serialization.uri.decodeURIComponent"))
fun decodeURIComponent(content: String) = com.lightningkite.kotlinx.serialization.uri.decodeURIComponent(content)