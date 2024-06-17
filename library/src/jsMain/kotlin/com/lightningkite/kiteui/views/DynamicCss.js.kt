package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Font
import com.lightningkite.kiteui.navigation.basePath
import kotlinx.browser.document
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.css.get


external interface BaseUrlScript {
    val baseUrl: String
}

actual class DynamicCss actual constructor(actual val basePath: String) {
    val customStyleSheet: CSSStyleSheet by lazy {
        val sheet = document.createElement("style") as HTMLStyleElement
        sheet.title = "generated-css"
        document.head!!.appendChild(sheet)
        document.styleSheets.let {
            for (i in 0 until it.length) {
                val copy = it.get(i)!!
                if (copy.title == sheet.title) return@let copy as CSSStyleSheet
            }
            throw IllegalStateException()
        }
    }

    constructor():this(
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
        return customStyleSheet.insertRule(rule, index)
    }

    actual fun styles(mediaQuery: String?, styles: List<Pair<String, Map<String, String>>>) {
        if (mediaQuery == null) styles.forEach { style(it.first, it.second) }
        else {
            val subrules = styles.sortedBy { it.first }.joinToString(" ") {
                """${it.first} { ${it.second.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }"""
            }
            rule(
                """@media $mediaQuery { $subrules }""",
                0
            )
        }
    }

    private val styleOnces = HashSet<String>()
    actual fun styleIfMissing(selector: String, map: Map<String, String>) {
        if(styleOnces.add(selector)) {
            val wrapSelector = selector//":not(.unkiteui) $selector"
            rule(
                """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }""",
                0
            )
        }
    }

    actual fun style(selector: String, map: Map<String, String>) {
        val wrapSelector = selector//":not(.unkiteui) $selector"
        rule(
            """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }""",
            0
        )
    }

    actual fun tempStyle(selector: String, map: Map<String, String>): () -> Unit {
        val wrapSelector = selector//":not(.unkiteui) $selector"
        val content = """$wrapSelector { ${map.entries.joinToString("; ") { "${it.key}: ${it.value}" }} }"""
        rule(
            content,
            0
        )
        val rule = customStyleSheet.cssRules.get(0)
        return {
            customStyleSheet.cssRules.let {
                (
                        0..<it.length).find { index -> it.get(index) === rule }
            }?.let {
                customStyleSheet.deleteRule(it)
            }
        }
    }
}