package com.lightningkite.kiteui.ssr

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.flat2
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.navigation.UrlLikePath
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.direct.text
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * SsrRouter.render/renderWithPreload/getPage each parse an incoming HTTP request path via
 * Routes.parse() before anything else runs - see commit 73ea45b85 / b85c01e86. No example-app
 * page has a non-String-backed typed route parameter (ArgumentsExamplePage's IdWrapper and
 * SsrResourceExamplePage's userId are both String underneath, so neither can fail to decode), so
 * a real generated parser can't be driven into throwing here. This test stands a hand-written
 * parser in for the generated one to reproduce what a bad segment for a typed parameter (e.g. an
 * Int) does: throw during decode. Before the fix, that exception propagated out of SsrRouter and
 * would have surfaced as a 500 to the requester instead of a 404/null.
 */
class SsrRouterTest {

    private object MatchPage : Page {
        override fun ElementWriter.CanAddTheme.render() {
            text("matched")
        }
    }

    private val theme = Theme.flat2("test", hue = 0.6.turns)

    /** Stands in for a generated parser on `items/{id}` where `id: Int`. */
    private fun routesWithThrowingParser(): Routes = Routes(
        parsers = listOf { path: UrlLikePath ->
            if (path.segments.firstOrNull() == "items") {
                path.segments[1].toInt() // throws NumberFormatException on a non-numeric segment
                MatchPage
            } else null
        },
        renderers = emptyMap()
    )

    @Test
    fun getPageReturnsNullRatherThanThrowingOnAMalformedTypedSegment() {
        val router = SsrRouter(routes = routesWithThrowingParser(), theme = theme)
        assertNull(router.getPage("/items/not-a-number"))
    }

    @Test
    fun renderReturnsNullRatherThanThrowingOnAMalformedTypedSegment() {
        val router = SsrRouter(routes = routesWithThrowingParser(), theme = theme)
        assertNull(router.render("/items/not-a-number"))
    }

    @Test
    fun renderWithPreloadReturnsNullRatherThanThrowingOnAMalformedTypedSegment() = runBlocking {
        val router = SsrRouter(routes = routesWithThrowingParser(), theme = theme)
        assertNull(router.renderWithPreload("/items/not-a-number"))
    }

    @Test
    fun aWellFormedSegmentStillResolvesThroughTheSameParser() {
        // Regression check: the try/catch guard added around parse() must not swallow a
        // legitimate match.
        val router = SsrRouter(routes = routesWithThrowingParser(), theme = theme)
        val html = router.render("/items/42")
        assertTrue(html != null && html.contains("matched"))
    }
}
