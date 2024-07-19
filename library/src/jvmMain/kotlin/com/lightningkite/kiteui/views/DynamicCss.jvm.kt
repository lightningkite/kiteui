package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font

actual class DynamicCss actual constructor(basePath: String) {
    actual val basePath: String = basePath

    actual fun font(font: Font): String {
        TODO("Not yet implemented")
    }

    actual fun rule(rule: String, index: Int): Int {
        TODO("Not yet implemented")
    }

    actual fun styleIfMissing(selector: String, map: Map<String, String>) {
    }

    actual fun style(selector: String, map: Map<String, String>) {
    }

    actual fun tempStyle(
        selector: String,
        map: Map<String, String>
    ): () -> Unit {
        TODO("Not yet implemented")
    }

    actual fun styles(
        mediaQuery: String?,
        styles: Map<String, Map<String, String>>
    ) {
    }

    actual fun flush() {}
}