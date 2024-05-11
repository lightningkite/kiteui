package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font

actual object DynamicCss {
    actual val basePath: String
        get() = TODO("Not yet implemented")

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
}