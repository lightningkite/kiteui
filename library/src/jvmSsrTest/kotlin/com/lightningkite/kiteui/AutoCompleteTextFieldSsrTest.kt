@file:OptIn(ExperimentalCoroutinesApi::class, OverrideOnly::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.AutoCompleteTextField
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.kiteui.views.direct.autoCompleteTextField
import com.lightningkite.kiteui.views.direct.col
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The `<datalist>` wiring [AutoCompleteTextField.suggestions] emits, checked in the markup
 * server-side rendering produces.
 *
 * Before this test existed, the `suggestions` setter on web stored the value and did nothing else -
 * the `<datalist>` wiring was commented out - so an autocomplete field silently offered no
 * completions at all. These tests pin down that the `<input>` is actually wired to a `<datalist>` via
 * matching `list`/`id` attributes, and that a suggestion cannot use markup characters to break out of
 * its `<option>` and inject content into the surrounding page.
 */
class AutoCompleteTextFieldSsrTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun renderHtml(build: ViewWriter.() -> Unit): String {
        val writer = Frame(ElementContext("/"))
        with(writer) { col { build() } }
        return buildString { writer.children[0].native.render(this) }
    }

    @Test
    fun suggestionsProduceADatalistWiredToTheInputByListId() {
        val html = renderHtml {
            autoCompleteTextField { suggestions = listOf("Apple", "Banana", "Cherry") }
        }

        val listAttr = Regex("""list='([^']+)'""").find(html)?.groupValues?.get(1)
        assertTrue(listAttr != null, "the <input> should declare a list= attribute pointing at a datalist: $html")

        assertTrue("<datalist id='$listAttr'" in html, "no <datalist> with a matching id was rendered: $html")
        for (suggestion in listOf("Apple", "Banana", "Cherry")) {
            assertTrue(
                "value='$suggestion'" in html,
                "expected an <option value='$suggestion'> inside the datalist: $html",
            )
        }
    }

    @Test
    fun anEmptyInputRendersNoDatalistOptions() {
        val html = renderHtml { autoCompleteTextField { suggestions = listOf() } }

        assertTrue("<datalist" in html, "a datalist should still be present even with an explicit empty suggestions list: $html")
        assertFalse("<option" in html, "no options should be rendered when suggestions is empty: $html")
    }

    @Test
    fun aFieldThatNeverUsesSuggestionsRendersNoDatalistAtAll() {
        // The datalist is created lazily on the first `suggestions` write, not eagerly - a plain
        // text field that never touches `suggestions` should not pay for (or emit) one at all.
        val html = renderHtml { autoCompleteTextField { hint = "Just a plain field" } }

        assertFalse("<datalist" in html, "no datalist should be emitted for a field that never sets suggestions: $html")
        assertFalse("list=" in html, "the input should not declare a list= attribute pointing nowhere: $html")
    }

    /**
     * A writer whose `willAddChild` never assigns a parent - the same shape as the real root
     * writers in root.kt (jsMain) and rootSetupIos.kt's `kiteUi()`: both attach the single root
     * element directly to the platform's native root (document.body / UIViewController.view)
     * without going through the normal ContainerElement.willAddChild that sets `Element.parent`.
     * Writing an AutoCompleteTextField as a page's sole root element (no wrapping `col`/`frame`)
     * hits this exact writer shape, so `parent` really can be null - this is not a hypothetical.
     */
    private fun renderAsSoleRootElement(setup: AutoCompleteTextField.() -> Unit): AutoCompleteTextField {
        val writer = object : ElementWriter, CoroutineScope by CoroutineScope(Dispatchers.IO) {
            override val context: ElementContext = ElementContext("/")
            override fun willAddChild(element: Element) {}
            override fun addChild(element: Element) {}
        }
        return with(writer) { autoCompleteTextField(setup) }
    }

    @Test
    fun aRootElementWithNoParentWorksFineAsLongAsSuggestionsIsNeverUsed() {
        // Must not throw: plain text-field usage does not need a parent container at all.
        val field = renderAsSoleRootElement { hint = "Just a plain field" }
        assertEquals("Just a plain field", field.hint)
    }

    @Test
    fun settingSuggestionsOnARootElementWithNoParentFailsLoudlyInsteadOfSilentlyDoingNothing() {
        // This is the failure mode the parent-null case must NOT fall into: silently discarding
        // the suggestions list is exactly the bug this whole file exists to catch, just triggered
        // by a missing parent instead of a missing <datalist> implementation. Fail fast instead.
        val exception = assertFailsWith<IllegalStateException> {
            renderAsSoleRootElement { suggestions = listOf("Apple", "Banana") }
        }
        assertTrue(
            "parent" in exception.message.orEmpty(),
            "the exception should explain that a parent container is missing: ${exception.message}",
        )
    }

    @Test
    fun reassigningSuggestionsReplacesThePreviousOptionsRatherThanAppending() {
        val html = renderHtml {
            autoCompleteTextField {
                suggestions = listOf("First")
                suggestions = listOf("Second")
            }
        }

        assertFalse("value='First'" in html, "the first assignment's option should have been cleared: $html")
        assertTrue("value='Second'" in html, "the latest assignment's option should be present: $html")
    }

    @Test
    fun suggestionsContainingMarkupCannotBreakOutOfTheOptionsAttribute() {
        // A suggestion string is caller-supplied data, not markup. If it were interpolated
        // unescaped, this value could close the `value` attribute early and inject a script tag
        // into the page the datalist renders into.
        val malicious = """"><script>alert(1)</script>"""
        val html = renderHtml {
            autoCompleteTextField { suggestions = listOf(malicious) }
        }

        assertFalse("<script>" in html, "the suggestion's markup must not survive unescaped: $html")
        assertFalse(
            """value='"><script>""" in html,
            "the suggestion must not be able to close the value attribute early: $html",
        )
        // The escaped form should still be present as the option's (harmless) value text.
        assertTrue("&lt;script&gt;" in html, "the escaped suggestion text should still be rendered: $html")
    }
}
