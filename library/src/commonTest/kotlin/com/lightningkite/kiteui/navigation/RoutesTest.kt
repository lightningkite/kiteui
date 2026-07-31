package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Routes.parse() is the last line of defense between generated route parsers and every caller
 * that receives a URL from outside the application - an HTTP request path, an OS deep link, a
 * restored navigation stack, a link inside user content. Generated parsers decode path segments
 * into typed parameters and throw on a segment that doesn't decode (e.g. `/items/abc` where an
 * Int is expected), so parse() must report that as "no route matched" rather than letting the
 * exception escape. See commit 73ea45b85 (added the guard as parseOrNull) and b85c01e86 (moved
 * it into parse() itself once every caller turned out to need it).
 *
 * Before 73ea45b85, `parse()` was `parsers.asSequence().mapNotNull { it(path) }.firstOrNull()`
 * with no try/catch, so [malformedSegmentDoesNotThrowFromParse] would have thrown
 * NumberFormatException instead of returning null.
 */
class RoutesTest {

    private object MatchPage : Page {
        override fun ElementWriter.CanAddTheme.render() {}
    }

    private val fallbackPage = Page.Direct("Fallback") {}

    /** Stands in for a generated parser on `items/{id}` where `id: Int`. */
    private fun throwingParser(): (UrlLikePath) -> Page? = { path ->
        if (path.segments.firstOrNull() == "items") {
            path.segments[1].toInt() // throws NumberFormatException on a non-numeric segment
            MatchPage
        } else null
    }

    @Test
    fun malformedSegmentDoesNotThrowFromParse() {
        val routes = Routes(parsers = listOf(throwingParser()), renderers = emptyMap(), fallback = fallbackPage)
        val result = routes.parse(UrlLikePath(listOf("items", "not-a-number"), emptyMap()))
        assertNull(result, "A path a generated parser can't decode must report as unmatched, not throw")
    }

    @Test
    fun malformedSegmentFallsBackToFallbackPage() {
        val routes = Routes(parsers = listOf(throwingParser()), renderers = emptyMap(), fallback = fallbackPage)
        val result = routes.parseOrFallback(UrlLikePath(listOf("items", "not-a-number"), emptyMap()))
        assertEquals(fallbackPage, result)
    }

    @Test
    fun wellFormedSegmentStillResolvesThroughTheSameParser() {
        // Regression check: the try/catch guard must not swallow a legitimate match.
        val routes = Routes(parsers = listOf(throwingParser()), renderers = emptyMap(), fallback = fallbackPage)
        assertEquals(MatchPage, routes.parse(UrlLikePath(listOf("items", "42"), emptyMap())))
    }

    @Test
    fun unrelatedPathIsUnaffectedByAnUnrelatedThrowingParser() {
        val routes = Routes(
            parsers = listOf(
                throwingParser(),
                { path: UrlLikePath -> if (path.segments == listOf("other")) MatchPage else null }
            ),
            renderers = emptyMap(),
            fallback = fallbackPage
        )
        assertEquals(MatchPage, routes.parse(UrlLikePath(listOf("other"), emptyMap())))
    }
}
