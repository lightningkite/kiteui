package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font

public actual class DynamicCss public actual constructor(basePath: String) {
    public actual val basePath: String = basePath
    public actual fun font(font: Font): String {
        TODO("Not yet implemented")
    }

    public actual fun rule(rule: String, index: Int): Int {
        TODO("Not yet implemented")
    }

    public actual fun emit(): String {
        TODO("Not yet implemented")
    }

    public actual fun add(selector: String, key: String, value: String, media: String) {
    }

    public actual fun flush() {
    }
}