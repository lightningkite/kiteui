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
 * Only navigate() should add a browser history entry.  replace() and goBack() used to push one too,
 * so the browser's Back button returned to pages the app had already left, like a just-saved form.
 */
class NavigatorHistoryTest {

    private data class NamedPage(val name: String) : Page {
        override fun ElementWriter.CanAddTheme.render() { space() }
    }

    @Test
    fun onlyNavigateAddsHistoryEntries() = GlobalScope.promise {
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
        assertEquals(listOf<Page>(NamedPage("list")), navigator.stack.value)
        val startLength = window.history.length

        navigator.navigate(NamedPage("edit"))
        assertEquals(startLength + 1, window.history.length)
        assertEquals("/edit", window.location.pathname)

        navigator.goBack()
        assertEquals(startLength + 1, window.history.length)
        assertEquals("/list", window.location.pathname)

        navigator.navigate(NamedPage("form"))
        assertEquals(startLength + 2, window.history.length)
        navigator.replace(NamedPage("detail"))
        assertEquals(startLength + 2, window.history.length)
        assertEquals("/detail", window.location.pathname)

        // Back from the saved detail page must not land on the form it replaced.
        window.history.back()
        var waited = 0
        while (window.location.pathname != "/list" && waited < 2000) {
            delay(50); waited += 50
        }
        assertEquals("/list", window.location.pathname)
        assertEquals(NamedPage("list"), navigator.stack.value.last())
    }
}
