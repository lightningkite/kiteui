package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.elementTree
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.direct.textInput
import com.lightningkite.kiteui.views.l2.navigatorView
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Showing a page must never steal keyboard focus. Automatically focusing the page's first field
 * pops the soft keyboard on Android and iOS the instant a screen appears, covering the content the
 * user just navigated to. Pages that genuinely want a focused field call `requestFocus` themselves.
 *
 * The screen reader still has to be told the screen changed, which `announceAsNewScreen` does
 * without touching keyboard focus - on the HTML platforms by making the page container
 * programmatically focusable and focusing it.
 *
 * Focus is observed through the `autofocus` attribute that the HTML-family `requestFocus` sets
 * synchronously; [focusIsObservableAsAnAutofocusRequest] pins that observation down so these tests
 * can't pass merely because the mechanism stopped recording.
 *
 * Anything touching the element tree happens inside the `elementTree` / `uiTest` block, which is
 * where `Dispatchers.Main` is installed.
 */
class NavigationFocusTest {

    private object FormPage : Page {
        override fun ElementWriter.CanAddTheme.render() {
            textInput { debugName = "firstField" }
        }
    }

    private object SecondFormPage : Page {
        override fun ElementWriter.CanAddTheme.render() {
            textInput { debugName = "secondField" }
        }
    }

    private fun emptyNavigator() = PageNavigator { Routes(parsers = emptyList(), renderers = emptyMap()) }

    private fun autofocus(element: NativeElement): String? = element.native.attributes["autofocus"]

    private fun programmaticallyFocusable(element: NativeElement): String? = element.native.attributes["tabindex"]

    @Test
    fun focusIsObservableAsAnAutofocusRequest() {
        var beforeRequest: String? = "unset"
        lateinit var field: NativeElement
        elementTree {
            field = textInput { debugName = "firstField" }
            beforeRequest = autofocus(field)
            field.requestFocus()
        }
        assertNull(beforeRequest, "Nothing should have asked for focus before requestFocus() was called")
        assertEquals("true", autofocus(field), "requestFocus() must be observable as an autofocus request")
    }

    @Test
    fun showingTheFirstPageDoesNotFocusItsFirstField() {
        val tree = elementTree {
            val navigator = emptyNavigator()
            navigator.stack.value = listOf(FormPage)
            navigatorView(navigator)
        }
        val field = tree.findByName("firstField") as NativeElement
        assertNull(autofocus(field), "The first page's first field must not be focused automatically")
    }

    @Test
    fun showingAPageMovesTheScreenReaderToThePageContainer() {
        val tree = elementTree {
            val navigator = emptyNavigator()
            navigator.stack.value = listOf(FormPage)
            navigatorView(navigator)
        }
        val container = tree.findByName("navigatorView") as NativeElement
        assertEquals(
            "-1",
            programmaticallyFocusable(container),
            "The page container must be focused so the screen reader follows the screen change, and " +
                    "kept out of the tab order while doing it",
        )
    }

    @Test
    fun everyNavigationMovesTheScreenReaderAgain() {
        lateinit var navigator: PageNavigator
        lateinit var container: NativeElement
        // The focus move is posted rather than immediate, so this needs uiTest's real dispatch loop
        // rather than the synchronous elementTree harness.
        uiTest(content = {
            navigator = emptyNavigator()
            navigator.stack.value = listOf(FormPage)
            container = navigatorView(navigator)
        }) {
            delay(100)
            assertEquals(1, container.native.focusCount, "Showing the first page must move the screen reader")
            navigator.navigate(SecondFormPage)
            delay(100)
            assertEquals(2, container.native.focusCount, "Each navigation must move the screen reader again")
            navigator.goBack()
            delay(100)
            assertEquals(3, container.native.focusCount, "Going back must move the screen reader too")
        }
    }

    @Test
    fun navigatingToAnotherPageDoesNotFocusItsFirstField() {
        val tree = elementTree {
            val navigator = emptyNavigator()
            navigator.stack.value = listOf(FormPage)
            navigatorView(navigator)
            navigator.navigate(SecondFormPage)
        }
        val field = tree.findByName("secondField") as NativeElement
        assertNull(autofocus(field), "A newly navigated-to page's first field must not be focused automatically")
    }
}
