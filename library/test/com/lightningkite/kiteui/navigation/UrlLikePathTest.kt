package com.lightningkite.kiteui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Locks down that [UrlLikePath.render] and [UrlLikePath.fromUrlString] are symmetric:
 * rendering a path to a URL string and parsing that string back must reproduce the
 * original path exactly, even when segments/keys/values contain characters that are
 * significant to URL syntax ('/', '&', '=', '%', space) or are non-ASCII.
 */
class UrlLikePathTest {

    private fun assertRoundTrips(path: UrlLikePath) {
        val rendered = path.render()
        val reparsed = UrlLikePath.fromUrlString(rendered)
        assertEquals(path, reparsed, "Round-trip through '$rendered' did not reproduce the original path")
    }

    @Test
    fun plainSegmentsAndParamsRoundTrip() {
        assertRoundTrips(UrlLikePath(listOf("items", "42"), mapOf("sort" to "name")))
    }

    @Test
    fun segmentWithSlashRoundTrips() {
        // A path variable containing a literal '/' must not be split into extra segments.
        assertRoundTrips(UrlLikePath(listOf("a/b", "c"), mapOf()))
    }

    @Test
    fun segmentWithSpaceRoundTrips() {
        assertRoundTrips(UrlLikePath(listOf("hello world"), mapOf()))
    }

    @Test
    fun segmentWithPercentRoundTrips() {
        // A raw '%' that isn't part of a valid escape sequence must survive unchanged.
        assertRoundTrips(UrlLikePath(listOf("100%done"), mapOf()))
    }

    @Test
    fun segmentWithUnicodeRoundTrips() {
        assertRoundTrips(UrlLikePath(listOf("café", "日本語"), mapOf()))
    }

    @Test
    fun queryKeyWithSpecialCharsRoundTrips() {
        // Key contains '=', '&', and a space - all of which are query-string delimiters.
        assertRoundTrips(UrlLikePath(listOf("search"), mapOf("a=b&c d" to "value")))
    }

    @Test
    fun queryValueWithSpecialCharsRoundTrips() {
        assertRoundTrips(UrlLikePath(listOf("search"), mapOf("q" to "a=b&c d%e日本語")))
    }

    @Test
    fun multipleParamsRoundTrip() {
        assertRoundTrips(
            UrlLikePath(
                listOf("search"),
                mapOf("a=1" to "x&y", "café" to "日本語", "plain" to "value")
            )
        )
    }
}
