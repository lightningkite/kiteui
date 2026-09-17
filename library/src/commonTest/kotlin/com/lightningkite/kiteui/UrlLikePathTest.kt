package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.UrlLikePath
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * URL parsing for routing.
 *
 * These strings arrive from outside the application - OS deep links, HTTP request lines,
 * restored navigation stacks - so both the splitting rules and the failure behavior matter.
 */
class UrlLikePathTest {

    @Test
    fun pathAndQueryAreSplitOnTheFirstQuestionMarkOnly() {
        // A literal '?' is legal inside the query per RFC 3986. Splitting on every occurrence
        // silently discarded everything after the second one.
        val path = UrlLikePath.fromUrlString("/page?redirect=/other?foo=bar")
        assertEquals(listOf("page"), path.segments)
        assertEquals("/other?foo=bar", path.parameters["redirect"])
    }

    @Test
    fun aPlainPathParsesWithNoParameters() {
        val path = UrlLikePath.fromUrlString("/docs/getting-started")
        assertEquals(listOf("docs", "getting-started"), path.segments)
        assertEquals(emptyMap(), path.parameters)
    }

    @Test
    fun multipleQueryParametersAreParsed() {
        val path = UrlLikePath.fromUrlString("/users/123?tab=profile&sort=name")
        assertEquals(listOf("users", "123"), path.segments)
        assertEquals("profile", path.parameters["tab"])
        assertEquals("name", path.parameters["sort"])
    }

    @Test
    fun anEmptyPathYieldsNoSegments() {
        assertEquals(emptyList(), UrlLikePath.fromUrlString("").segments)
        assertEquals(emptyList(), UrlLikePath.fromUrlString("/").segments)
    }

    @Test
    fun aTrailingQuestionMarkYieldsNoParameters() {
        val path = UrlLikePath.fromUrlString("/page?")
        assertEquals(listOf("page"), path.segments)
        assertEquals(emptyMap(), path.parameters)
    }

    @Test
    fun renderRoundTripsThroughFromUrlString() {
        val original = UrlLikePath(listOf("a", "b"), mapOf("k" to "v"))
        val reparsed = UrlLikePath.fromUrlString(original.render())
        assertEquals(original.segments, reparsed.segments)
        assertEquals(original.parameters, reparsed.parameters)
    }
}
