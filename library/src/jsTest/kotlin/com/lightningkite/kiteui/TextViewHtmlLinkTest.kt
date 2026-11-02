package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.views.direct.TextView
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.events.MouseEventInit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextViewHtmlLinkTest {
    private val product = Page.Direct("Product") {}
    private val navigator = PageNavigator {
        Routes(parsers = listOf { path -> product.takeIf { path.segments == listOf("Product", "1") } }, renderers = mapOf())
    }

    /** Clicks the link in [html] and reports whether the page handled it, stopping the browser from following it either way. */
    private fun click(html: String, ctrlKey: Boolean = false): Boolean {
        lateinit var view: TextView
        root(Theme(id = "unitTest")) {
            context.pageNavigator = navigator
            col { text { view = this; setBasicHtmlContent(html) } }
        }
        val anchor = (view.native.element as HTMLElement).querySelector("a") as HTMLAnchorElement
        var handled = false
        val record: (org.w3c.dom.events.Event) -> Unit = { handled = it.defaultPrevented; it.preventDefault() }
        document.addEventListener("click", record)
        anchor.dispatchEvent(MouseEvent("click", MouseEventInit(bubbles = true, cancelable = true, ctrlKey = ctrlKey)))
        document.removeEventListener("click", record)
        return handled
    }

    @Test
    fun aLinkIntoTheAppNavigatesWithoutReloading() {
        assertTrue(click("See (<a href=\"/Product/1\">the product</a>)"))
        assertEquals(listOf<Page>(product), navigator.stack.value)
    }

    @Test
    fun aLinkElsewhereIsLeftToTheBrowser() {
        assertFalse(click("<a href=\"https://example.com/Product/1\">elsewhere</a>"))
        assertFalse(click("<a href=\"/Unknown\">unknown</a>"))
        assertEquals(listOf(), navigator.stack.value)
    }

    @Test
    fun aModifierClickIsLeftToTheBrowserForANewTab() {
        assertFalse(click("<a href=\"/Product/1\">the product</a>", ctrlKey = true))
        assertEquals(listOf(), navigator.stack.value)
    }
}
