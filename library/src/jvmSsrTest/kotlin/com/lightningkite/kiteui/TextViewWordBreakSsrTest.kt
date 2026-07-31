@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `TextView.wordBreak` under SSR.
 *
 * Same underlying implementation as the web target (both live in commonHtmlMain), so the same
 * bug applied here: the getter was `TODO("Not yet implemented")`, meaning any server-side code
 * that read the property back - not just set it - crashed rendering outright.
 */
class TextViewWordBreakSsrTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun renderHtml(build: ViewWriter.() -> Unit): String {
        val writer = Frame(ElementContext("/"))
        with(writer) { col { build() } }
        return buildString { writer.children[0].native.render(this) }
    }

    @Test
    fun freshTextViewReadsBackNormal() {
        renderHtml {
            text {
                // The bare read below is what used to throw NotImplementedError before the fix.
                assertEquals(WordBreak.Normal, wordBreak, "a fresh TextView must default to Normal")
            }
        }
    }

    @Test
    fun settingBreakAllRoundTripsThroughTheGetter() {
        renderHtml {
            text {
                wordBreak = WordBreak.BreakAll
                assertEquals(WordBreak.BreakAll, wordBreak, "reading back the value just set must not throw")
            }
        }
    }

    @Test
    fun breakAllIsReflectedInTheRenderedStyle() {
        val html = renderHtml { text { wordBreak = WordBreak.BreakAll } }
        assertTrue(html.contains("word-break:break-all"), "expected the break-all CSS in the SSR html: $html")
    }

    @Test
    fun normalIsReflectedInTheRenderedStyle() {
        val html = renderHtml { text { wordBreak = WordBreak.BreakAll; wordBreak = WordBreak.Normal } }
        assertTrue(html.contains("word-break:normal"), "expected normal CSS after switching back: $html")
    }
}
