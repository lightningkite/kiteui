package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font
import com.lightningkite.kiteui.navigation.basePath
import kotlinx.browser.document
import kotlinx.dom.appendText
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.css.get
import kotlin.js.Json
import kotlin.js.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime


external interface BaseUrlScript {
    val baseUrl: String
}

actual class DynamicCss actual constructor(actual val basePath: String) {
    val customStyleSheetElement: HTMLStyleElement by lazy {
        val sheet = document.createElement("style") as HTMLStyleElement
        sheet.title = "generated-css"
        document.head!!.appendChild(sheet)
        sheet
    }
    val customStyleSheet: CSSStyleSheet by lazy {
        customStyleSheetElement
        document.styleSheets.let {
            for (i in 0 until it.length) {
                val copy = it.get(i)!!
                if (copy.title == customStyleSheetElement.title) return@let copy as CSSStyleSheet
            }
            throw IllegalStateException()
        }
    }

    constructor() : this(
        (document.getElementById("baseUrlLocation") as? HTMLScriptElement)?.innerText?.let {
            JSON.parse<BaseUrlScript>(it).baseUrl
        } ?: "/"
    )

    private val fontHandled = HashSet<String>()
    actual fun font(font: Font): String {
        if (!fontHandled.add(font.cssFontFamilyName)) return font.cssFontFamilyName
        if (font.url != null) {
            document.head!!.appendChild((document.createElement("link") as HTMLLinkElement).apply {
                rel = "stylesheet"
                type = "text/css"
                href = font.url
            })
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
        try {
            return customStyleSheet.insertRule(rule, index)
        } catch (e: Throwable) {
            throw Exception("Failed to add rule $rule", e)
        }
    }

    var queue: Json = json()
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    private fun Json.subObj(key: String) = this.get(key) as? Json ?: run {
        val obj = json()
        this.set(key, obj)
        obj
    }

    private fun jsonForEach(obj: Json, action: (key: String, value: Any?) -> Unit) =
        js("for (var key in obj) { action(key, obj[key]) }")

    actual fun add(selector: String, key: String, value: String, media: String) {
        queue.subObj(media).subObj(selector).set(key, value)
    }
    actual fun emit(): String {
        return customStyleSheet.cssRules.let {
            (0..<it.length).asSequence().mapNotNull { i -> it.get(i) }.joinToString("\n") { it.cssText }
        }
    }

    var flushTotal: Duration = 0.seconds
    var ruleTotal = 0
    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    actual fun flush() {
        measureTime {
//            val map = HashMap<String, HashMap<String, HashMap<String, String>>>()
//            jsonForEach(queue) { media, it ->
//                jsonForEach(it as Json) { selector, it ->
//                    jsonForEach(it as Json) { key, value ->
//                        map.getOrPut(media) { HashMap() }.getOrPut(selector) { HashMap() }.put(key, value as String)
//                    }
//                }
//            }
//
//            val merged = map.mapValues {
//                val merg = it.value.entries.groupBy { it.value }.values
//                merg.associate { it.map { it.key }.joinToString() to it.first().value }
//            }
//
//            merged.forEach { (media, it) ->
//                var str = "@media $media {"
//                it.forEach { (selector, it) ->
//                    str += selector
//                    str += "{"
//                    it.forEach { (key, value) ->
//                        str += key
//                        str += ":"
//                        str += value
//                        str += ";"
//                    }
//                    str += "}"
//                    ruleTotal++
//                }
//                str += "}"
//                rule(str, 0)
//            }
//            queue = json()
            jsonForEach(queue) { media, it ->
                var str = "@media $media {"
                jsonForEach(it as Json) { selector, it ->
                    str += selector
                    str += "{"
                    jsonForEach(it as Json) { key, value ->
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
            queue = json()
        }.also {
            flushTotal += it
        }
    }
}

//actual class DynamicCss actual constructor(actual val basePath: String) {
//    val customStyleSheetElement: HTMLStyleElement by lazy {
//        val sheet = document.createElement("style") as HTMLStyleElement
//        sheet.title = "generated-css"
//        document.head!!.appendChild(sheet)
//        sheet
//    }
//    val customStyleSheet: CSSStyleSheet by lazy {
//        customStyleSheetElement
//        document.styleSheets.let {
//            for (i in 0 until it.length) {
//                val copy = it.get(i)!!
//                if (copy.title == customStyleSheetElement.title) return@let copy as CSSStyleSheet
//            }
//            throw IllegalStateException()
//        }
//    }
//
//    constructor():this(
//        (document.getElementById("baseUrlLocation") as? HTMLScriptElement)?.innerText?.let {
//            JSON.parse<BaseUrlScript>(it).baseUrl
//        } ?: "/"
//    )
//
//    private val fontHandled = HashSet<String>()
//    actual fun font(font: Font): String {
//        if (!fontHandled.add(font.cssFontFamilyName)) return font.cssFontFamilyName
//        if (font.url != null) {
//            document.head!!.appendChild((document.createElement("link") as HTMLLinkElement).apply {
//                rel = "stylesheet"
//                type = "text/css"
//                href = font.url
//            })
//        }
//        if (font.direct != null) {
//            font.direct.normal.forEach {
//                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: normal;font-weight: ${it.key};src:url('${basePath + it.value}');}")
//            }
//            font.direct.italics.forEach {
//                rule("@font-face {font-family: '${font.cssFontFamilyName}';font-style: italic;font-weight: ${it.key};src:url('${basePath + it.value}');}")
//            }
//        }
//        return font.cssFontFamilyName
//    }
//
//    actual fun rule(rule: String, index: Int): Int {
//        try {
//            return customStyleSheet.insertRule(rule, index)
//        } catch(e: Throwable) {
//            throw Exception("Failed to add rule $rule", e)
//        }
//    }
//
//    actual fun styles(mediaQuery: String?, styles: Map<String, Map<String, String>>) {
//        if (mediaQuery == null) {
//            styles.forEach { style(it.key, it.value) }
//        } else {
//            val subrules = styles.entries.sortedBy { it.key }.sortedBy { it.key }.joinToString(" ") {
//                """${it.key} { ${it.value.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }"""
//            }
//            rule(
//                """@media ${mediaQuery ?: ""} { $subrules }""",
//                0
//            )
//        }
//    }
//
//    private val styleOnces = HashSet<String>()
//    actual fun styleIfMissing(selector: String, map: Map<String, String>) {
//        if(styleOnces.add(selector)) {
//            val wrapSelector = selector//":not(.unkiteui) $selector"
//            rule(
//                """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }""",
//                0
//            )
//        }
//    }
//
//    actual fun style(selector: String, map: Map<String, String>) {
//        val wrapSelector = selector//":not(.unkiteui) $selector"
//        rule(
//            """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }""",
//            0
//        )
//    }
//
//    actual fun tempStyle(selector: String, map: Map<String, String>): () -> Unit {
//        val wrapSelector = selector//":not(.unkiteui) $selector"
//        val content = """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }"""
//        rule(
//            content,
//            0
//        )
//        val rule = customStyleSheet.cssRules.get(0)
//        return {
//            customStyleSheet.cssRules.let {
//                (
//                        0..<it.length).find { index -> it.get(index) === rule }
//            }?.let {
//                customStyleSheet.deleteRule(it)
//            }
//        }
//    }
//
//}