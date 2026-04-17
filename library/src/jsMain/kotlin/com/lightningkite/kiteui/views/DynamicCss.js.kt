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
            jsonForEach(queue) { media, it ->
                val wrapInMedia = media.isNotBlank()
                if (wrapInMedia) {
                    // Conditional @media block (e.g. print, hover) — wrap as usual
                    val str = StringBuilder("@media $media {")
                    jsonForEach(it as Json) { selector, it ->
                        str.append(selector)
                        str.append("{")
                        jsonForEach(it as Json) { key, value ->
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
                } else {
                    // Empty media query — emit each rule directly without @media wrapper.
                    // Chrome's SVG foreignObject renderer ignores CSS inside @media {} blocks
                    // when rendering SVGs as <img> elements, breaking DOM-to-image screenshots.
                    jsonForEach(it as Json) { selector, it ->
                        val str = StringBuilder(selector)
                        str.append("{")
                        jsonForEach(it as Json) { key, value ->
                            str.append(key)
                            str.append(":")
                            str.append(value)
                            str.append(";")
                        }
                        str.append("}")
                        ruleTotal++
                        rule(str.toString(), 0)
                    }
                }
            }
            queue = json()
        }.also {
            flushTotal += it
        }
    }
}

//}