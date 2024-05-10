package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.dom.HTMLLinkElement
import com.lightningkite.kiteui.dom.HTMLScriptElement
import com.lightningkite.kiteui.models.Font
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.kiteui.views.DynamicCSS.customStyleSheet
import kotlinx.browser.document
import org.w3c.dom.css.get


external interface BaseUrlScript {
    val baseUrl: String
}

actual object DynamicCss {

    actual val basePath: String = (document.getElementById("baseUrlLocation") as? HTMLScriptElement)?.innerText?.let {
        JSON.parse<BaseUrlScript>(it).baseUrl
    } ?: "/"

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