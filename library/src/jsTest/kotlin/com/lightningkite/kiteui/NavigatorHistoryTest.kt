@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.RouteRendered
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.navigation.bindToPlatform
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.space
import kotlinx.browser.window
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The browser's history should mirror the app's stack: browser Back after an in-app goBack() moves on in one
 * press, and Back from a page that replace()d a form skips the form.
 */
class NavigatorHistoryTest {

    private data class NamedPage(val name: String) : Page {
        override fun ElementWriter.CanAddTheme.render() { space() }
    }

    // Browser history moves asynchronously.
    private suspend fun awaitPath(path: String) {
        var waited = 0
        while (window.location.pathname != path && waited < 2000) {
            delay(50); waited += 50
        }
        delay(50)
        assertEquals(path, window.location.pathname)
    }

    @Test
    fun browserHistoryMirrorsTheStack() = GlobalScope.promise {
        window.history.replaceState(null, "", "/list")
        lateinit var context: ElementContext
        root(Theme(id = "unitTest")) { context = this.context }
        val navigator = PageNavigator {
            Routes(
                parsers = listOf { path -> NamedPage(path.segments.single()) },
                renderers = mapOf(NamedPage::class to { page -> RouteRendered(UrlLikePath(listOf((page as NamedPage).name), mapOf()), listOf()) }),
            )
        }
        navigator.bindToPlatform(context)
        val startLength = window.history.length

        navigator.navigate(NamedPage("a"))
        navigator.navigate(NamedPage("b"))
        assertEquals(startLength + 2, window.history.length)
        assertEquals("/b", window.location.pathname)

        navigator.goBack()
        awaitPath("/a")
        assertEquals(listOf<Page>(NamedPage("list"), NamedPage("a")), navigator.stack.value)

        // One press, not a dead step on a duplicate of /a.
        window.history.back()
        awaitPath("/list")
        assertEquals(NamedPage("list"), navigator.stack.value.last())

        // Forward returns to the page that was left, as on any website.
        window.history.forward()
        awaitPath("/a")
        assertEquals(NamedPage("a"), navigator.stack.value.last())

        navigator.navigate(NamedPage("form"))
        navigator.replace(NamedPage("detail"))
        assertEquals("/detail", window.location.pathname)

        // Back from the saved detail page must not land on the form it replaced.
        window.history.back()
        awaitPath("/a")
        assertEquals(NamedPage("a"), navigator.stack.value.last())
    }
}
