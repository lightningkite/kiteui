@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.markdown.markdown
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.ssr.SsrContext
import com.lightningkite.kiteui.views.direct.text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TextViewHtmlLinkTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private val product = Page.Direct("Product") {}
    private val navigator = PageNavigator {
        Routes(parsers = listOf { path -> product.takeIf { path.segments == listOf("Product", "1") } }, renderers = mapOf())
    }

    /** The opening tag of the link whose text is [text]. */
    private fun String.anchor(text: String): String = Regex("<a [^>]*>(?=$text</a>)").find(this)!!.value

    @Test
    fun aLinkTheAppCantShowOpensInANewTab() {
        val ssr = SsrContext("/")
        ssr.render {
            context.pageNavigator = navigator
            markdown("[product](/Product/1), [manual](/manuals/esp/index.html#wiring), [site](https://example.com)")
        }
        val html = ssr.serialize().html

        assertEquals("<a href=\"/Product/1\">", html.anchor("product"))
        for (outside in listOf("manual", "site")) {
            assertContains(html.anchor(outside), "target=\"_blank\"")
            assertContains(html.anchor(outside), "rel=\"noopener\"")
        }
    }

    @Test
    fun aMarkdownBlockLinkOutsideTheAppOpensInANewTab() {
        val ssr = SsrContext("/")
        ssr.render { markdown("[> # Linked Heading](https://example.com)") }
        val link = Regex("<a [^>]*href='https://example.com'[^>]*>").find(ssr.serialize().html)!!.value
        assertContains(link, "target='_blank'")
    }

    @Test
    fun anExplicitTargetIsKept() {
        val ssr = SsrContext("/")
        ssr.render {
            context.pageNavigator = navigator
            text { setBasicHtmlContent("<a href=\"https://example.com\" target=\"_self\">here</a>") }
        }
        assertEquals("<a href=\"https://example.com\" target=\"_self\">", ssr.serialize().html.anchor("here"))
    }

    @Test
    fun linksInRichTextTakeTheTextsColor() {
        val ssr = SsrContext("/")
        ssr.render { text { setBasicHtmlContent("<a href=\"https://example.com\">x</a>") } }
        assertContains(ssr.serialize().css, ".kui-basic-html-content a { color: inherit; }")
    }
}
