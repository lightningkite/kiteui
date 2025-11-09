package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font
import kotlin.collections.HashSet
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

actual class DynamicCss actual constructor(basePath: String) {
    val rules = java.util.Collections.synchronizedList(ArrayList<String>())
    val headElements = java.util.Collections.synchronizedList(ArrayList<String>())

    actual val basePath: String = basePath

    private val fontHandled = java.util.Collections.synchronizedSet(HashSet<String>())
    @Synchronized
    actual fun font(font: Font): String {
        if (!fontHandled.add(font.cssFontFamilyName)) return font.cssFontFamilyName
        if (font.url != null) {
            headElements += "<link rel=\"stylesheet\" href=\"${font.url}\" />"
        }
        if (font.direct != null) {
            font.direct.normal.forEach {
                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: normal;font-weight: ${it.key};src:url('${basePath + it.value}');}")
            }
            font.direct.italics.forEach {
                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: italic;font-weight: ${it.key};src:url('${basePath + it.value}');}")
            }
        }
        return font.cssFontFamilyName
    }

    actual fun rule(rule: String, index: Int): Int {
        rules += rule
        return rules.lastIndex
    }

    actual fun emit(): String {
        return rules.joinToString("\n")
    }

    val map = java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>>>()

    actual fun add(selector: String, key: String, value: String, media: String) {
        map.getOrPut(media) { java.util.concurrent.ConcurrentHashMap() }.getOrPut(selector) { java.util.concurrent.ConcurrentHashMap() }[key] = value
    }

    @Volatile var flushTotal: Duration = 0.seconds
    @Volatile var ruleTotal = 0
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    @Synchronized
    actual fun flush() {
        measureTime {
            val merged = map.mapValues {
                val merg = it.value.entries.groupBy { it.value }.values
                merg.associate { it.map { it.key }.joinToString() to it.first().value }
            }

            merged.forEach { (media, it) ->
                var str = "@media $media {"
                it.forEach { (selector, it) ->
                    str += selector
                    str += "{"
                    it.forEach { (key, value) ->
                        str += key
                        str += ":"
                        str += value
                        str += ";"
                    }
                    str += "}"
                    ruleTotal++
                }
                str += "}"
                rule(str, 0)
            }
        }.also {
            flushTotal += it
        }
    }
}