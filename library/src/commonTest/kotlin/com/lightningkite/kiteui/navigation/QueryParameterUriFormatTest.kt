package com.lightningkite.kiteui.navigation

import com.lightningkite.kotlinx.serialization.uri.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for a double-encoding bug where a multi-word @QueryParameter value (e.g. a
 * dealership search term with a space) came back in the URL bar as a literal `%2520` instead of
 * `%20`, so the term never matched anything server-side.
 *
 * `DefaultUriFormat.encodeToStringMap`/`decodeFromStringMap` (kotlinx-serialization-uri) percent-
 * encode/decode the values they write/read - see UriFormat.kt. `UrlLikePath.render()`/
 * `fromUrlString()` ALSO percent-encode/decode segments and parameters, because `UrlLikePath`'s own
 * contract is to hold raw, un-encoded values (see UrlLikePathTest). Generated route code
 * (gradle-plugin's generateRoutes.kt) bridges the two formats by decoding immediately after
 * `DefaultUriFormat`'s encode and re-encoding immediately before `DefaultUriFormat`'s decode, so the
 * percent-encoding happens exactly once end-to-end. This test exercises that exact glue pattern
 * (rather than either library in isolation) since the bug only appeared at the boundary between them.
 */
class QueryParameterUriFormatTest {

    @Test
    fun multiWordQueryValueRoundTripsThroughARenderedUrl() {
        // -- render side, mirrors the generated renderer for a @QueryParameter --
        val params = mutableMapOf<String, String>()
        DefaultUriFormat.encodeToStringMap("query", "Chevrolet of California", params)
        val urlLikePath = UrlLikePath(
            segments = listOf("dealerships"),
            parameters = params.mapValues { decodeURIComponent(it.value) }
        )

        // Extract+decode (rather than comparing the rendered string literally) so this test isn't
        // tied to a particular platform's encodeURIComponent flavor (e.g. JVM's URLEncoder uses '+'
        // for spaces, JS's encodeURIComponent uses '%20') - what matters is a single decode fully
        // recovers the original value. Double-encoded, a single decode would leave "%20" behind.
        val url = urlLikePath.render()
        val renderedValue = url.substringAfter("query=")
        assertEquals(
            "Chevrolet of California",
            decodeURIComponent(renderedValue),
            "A query value must be percent-encoded exactly once, not twice"
        )

        // -- parse side, mirrors the generated parser for a @QueryParameter --
        val reparsed = UrlLikePath.fromUrlString(url)
        val encodedParameters = reparsed.parameters.mapValues { (_, v) -> encodeURIComponent(v) }
        val decoded: String = DefaultUriFormat.decodeFromStringMap("query", encodedParameters)

        assertEquals("Chevrolet of California", decoded)
    }
}
