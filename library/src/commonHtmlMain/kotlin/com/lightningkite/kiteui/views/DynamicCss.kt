package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font

public expect class DynamicCss(basePath: String) {
    public val basePath: String
    public fun font(font: Font): String
    public fun rule(rule: String, index: Int = 0): Int
//    fun styleIfMissing(selector: String, map: Map<String, String>)
//    fun style(selector: String, map: Map<String, String>)
//    fun tempStyle(selector: String, map: Map<String, String>): () -> Unit
//    fun styles(
//        mediaQuery: String? = null,
//        styles: Map<String, Map<String, String>>
//    )
    public fun emit(): String
    public fun add(selector: String, key: String, value: String, media: String = "")
    public fun flush()
}