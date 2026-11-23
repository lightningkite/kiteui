package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font
import kotlin.collections.HashSet
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

public actual class DynamicCss actual constructor(basePath: String) {
    internal val rules: MutableList<String> = java.util.Collections.synchronizedList(ArrayList<String>())
    internal val headElements: MutableList<String> = java.util.Collections.synchronizedList(ArrayList<String>())

    public actual val basePath: String = basePath

    private val fontHandled = java.util.Collections.synchronizedSet(HashSet<String>())
    @Synchronized
    public actual fun font(font: Font): String {
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

    public actual fun rule(rule: String, index: Int): Int {
        rules += rule
        return rules.lastIndex
    }

    public actual fun emit(): String {
        return rules.joinToString("\n")
    }

    // media -> selector -> property -> value, added since the last flush.
    private var queue = mutableMapOf<String, MutableMap<String, MutableMap<String, String>>>()

    @Synchronized
    public actual fun add(selector: String, key: String, value: String, media: String) {
        queue.getOrPut(media) { mutableMapOf() }.getOrPut(selector) { mutableMapOf() }[key] = value
    }

    @Volatile internal var flushTotal: Duration = 0.seconds
    @Volatile internal var ruleTotal: Int = 0
    @Synchronized
    public actual fun flush() {
        measureTime {
            queue.forEach { (media, selectors) ->
                val str = StringBuilder("@media $media {")
                // Selectors with identical declarations share one rule.
                selectors.entries.groupBy({ it.value }, { it.key }).forEach { (declarations, sameSelectors) ->
                    sameSelectors.joinTo(str)
                    str.append("{")
                    declarations.forEach { (key, value) ->
                        str.append(key)
                        str.append(":")
                        str.append(value)
                        str.append(";")
                    }
                    str.append("}")
                    ruleTotal++
                }
                str.append("}")
                rule(str.toString(), 0)
            }
            queue = mutableMapOf()
        }.also {
            flushTotal += it
        }
    }
}