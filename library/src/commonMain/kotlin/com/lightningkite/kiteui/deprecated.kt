package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.NativeElementCommonCode

@Deprecated("Use dot syntax instead", level = DeprecationLevel.ERROR)
public operator fun Nothing.minus(other: Nothing): Nothing = TODO()
@Deprecated("Use dot syntax instead", level = DeprecationLevel.ERROR)
public operator fun Nothing.contains(other: Nothing): Boolean = TODO()

@Deprecated("Use kotlinx.coroutines instead") public val delay = Unit

@Deprecated("import has moved", ReplaceWith("encodeURIComponent(content)", "com.lightningkite.kotlinx.serialization.uri.encodeURIComponent"))
public fun encodeURIComponent(content: String) = com.lightningkite.kotlinx.serialization.uri.encodeURIComponent(content)

@Deprecated("import has moved", ReplaceWith("decodeURIComponent(content)", "com.lightningkite.kotlinx.serialization.uri.decodeURIComponent"))
public fun decodeURIComponent(content: String) = com.lightningkite.kotlinx.serialization.uri.decodeURIComponent(content)

@Deprecated("Use new 'Element' types", ReplaceWith("Element", "com.lightningkite.kiteui.views.Element"))
public typealias RView = Element

@Deprecated("Specify if this is a container or not")
public typealias RViewHelper = NativeElementCommonCode
