package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font

expect class DynamicCss(basePath: String) {
    val basePath: String
    fun font(font: Font): String
    fun rule(rule: String, index: Int = 0): Int
//    fun styleIfMissing(selector: String, map: Map<String, String>)
//    fun style(selector: String, map: Map<String, String>)
//    fun tempStyle(selector: String, map: Map<String, String>): () -> Unit
//    fun styles(
//        mediaQuery: String? = null,
//        styles: Map<String, Map<String, String>>
//    )
//    fun emit()
    fun add(selector: String, key: String, value: String, media: String = "")
    fun flush()
}