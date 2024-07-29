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

    actual fun emit(): String {
        TODO("Not yet implemented")
    }

    actual fun add(selector: String, key: String, value: String, media: String) {
    }

    actual fun flush() {
    }
}