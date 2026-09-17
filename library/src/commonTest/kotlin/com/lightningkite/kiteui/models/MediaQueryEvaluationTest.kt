package com.lightningkite.kiteui.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The native answer to a [MediaQuery] has to match what a browser would say for the same window,
 * because the whole point of `shownForQuery` is that shared UI code lays out identically in an app
 * and on the web. This pins the comparison directions - a flipped `>=` is invisible until someone
 * notices a breakpoint behaving backwards on one platform only.
 *
 * DO NOT copy the `.px` used below into a real query - see the warning on [MediaQuery.MinWidth],
 * which says to use `.dp` or `.rem`. `.px` appears here only because it is the one unit that can be
 * constructed in `commonTest`: `.dp` and `.rem` read `lateinit` fields on `AndroidAppContext` that
 * a plain unit test never initialises, so they throw on the Android target. The choice is safe for
 * this file specifically because the evaluator's arithmetic is unit-agnostic - every comparison
 * converts both sides the same way - so these assertions hold in any unit. That is emphatically not
 * true of a real query, where the unit decides which physical size the breakpoint lands on.
 */
class MediaQueryEvaluationTest {

    private val device = touchNativeDeviceTraits

    private fun window(width: Int, height: Int) =
        WindowStatistics(width = width.px, height = height.px, density = 1f)

    private fun MediaQuery.on(width: Int, height: Int) = matches(window(width, height), device)

    @Test
    fun widthBoundsAreInclusiveAndPointTheRightWay() {
        val min = MediaQuery.MinWidth(100.px)
        assertTrue(min.on(101, 50), "wider than the minimum should match")
        assertTrue(min.on(100, 50), "exactly the minimum should match")
        assertFalse(min.on(99, 50), "narrower than the minimum must not match")

        val max = MediaQuery.MaxWidth(100.px)
        assertTrue(max.on(99, 50), "narrower than the maximum should match")
        assertTrue(max.on(100, 50), "exactly the maximum should match")
        assertFalse(max.on(101, 50), "wider than the maximum must not match")
    }

    @Test
    fun heightBoundsAreInclusiveAndPointTheRightWay() {
        val min = MediaQuery.MinHeight(100.px)
        assertTrue(min.on(50, 100), "exactly the minimum should match")
        assertFalse(min.on(50, 99), "shorter than the minimum must not match")

        val max = MediaQuery.MaxHeight(100.px)
        assertTrue(max.on(50, 100), "exactly the maximum should match")
        assertFalse(max.on(50, 101), "taller than the maximum must not match")
    }

    @Test
    fun aspectRatioIsWidthOverHeight() {
        // A 2:1 landscape window. Getting this inverted is the classic mistake, so assert on a
        // non-square window where the two orderings genuinely disagree.
        assertTrue(MediaQuery.MinAspectRatio(1.5).on(200, 100), "2.0 is at least 1.5")
        assertFalse(MediaQuery.MinAspectRatio(2.5).on(200, 100), "2.0 is not at least 2.5")
        assertTrue(MediaQuery.MaxAspectRatio(2.5).on(200, 100), "2.0 is at most 2.5")
        assertFalse(MediaQuery.MaxAspectRatio(1.5).on(200, 100), "2.0 is not at most 1.5")
    }

    @Test
    fun aZeroHeightWindowMatchesNoAspectRatioQuery() {
        // Defensive against a window size we cannot currently produce - see the note on the
        // evaluator. Neither query may match, and neither may throw or produce a NaN comparison
        // that silently reads as false for the wrong reason.
        assertFalse(MediaQuery.MinAspectRatio(1.0).on(100, 0))
        assertFalse(MediaQuery.MaxAspectRatio(1.0).on(100, 0))
        assertFalse(MediaQuery.MinAspectRatio(1.0).on(0, 0))
    }

    @Test
    fun andRequiresEveryQueryAndOrRequiresOne() {
        val wide = MediaQuery.MinWidth(100.px)
        val tall = MediaQuery.MinHeight(100.px)

        assertTrue(MediaQuery.And(setOf(wide, tall)).on(200, 200))
        assertFalse(MediaQuery.And(setOf(wide, tall)).on(200, 50), "one failing member fails the And")

        assertTrue(MediaQuery.Or(setOf(wide, tall)).on(200, 50), "one passing member passes the Or")
        assertFalse(MediaQuery.Or(setOf(wide, tall)).on(50, 50), "no passing member fails the Or")
    }

    @Test
    fun emptyCombinatorsFollowCssConventions() {
        assertTrue(MediaQuery.And(emptySet()).on(100, 100), "an empty And is vacuously true")
        assertFalse(MediaQuery.Or(emptySet()).on(100, 100), "an empty Or is vacuously false")
    }

    @Test
    fun nestedCombinatorsEvaluateRecursively() {
        // Guards against a shallow implementation that only looks one level deep.
        val query = MediaQuery.And(
            setOf(
                MediaQuery.MinWidth(100.px),
                MediaQuery.Or(setOf(MediaQuery.MaxHeight(50.px), MediaQuery.MinHeight(500.px))),
            )
        )

        assertTrue(query.on(200, 40), "wide, and short enough for the inner Or")
        assertTrue(query.on(200, 600), "wide, and tall enough for the inner Or")
        assertFalse(query.on(200, 200), "wide, but the inner Or is unsatisfied")
        assertFalse(query.on(50, 40), "inner Or satisfied, but not wide enough")
    }

    @Test
    fun deviceFeaturesReportTheTouchFirstAnswers() {
        // These are what a mobile browser reports on the same hardware. If they ever diverge from
        // that, shared code would lay out differently in the app than on the web.
        assertTrue(MediaQuery.Pointer(MediaQuery.Pointer.Option.Coarse).on(100, 100))
        assertFalse(MediaQuery.Pointer(MediaQuery.Pointer.Option.Fine).on(100, 100))
        assertFalse(MediaQuery.Pointer(MediaQuery.Pointer.Option.None).on(100, 100))

        assertTrue(MediaQuery.Hover(MediaQuery.Hover.Option.None).on(100, 100))
        assertFalse(MediaQuery.Hover(MediaQuery.Hover.Option.Hover).on(100, 100))

        assertTrue(MediaQuery.Update(MediaQuery.Update.Option.Fast).on(100, 100))
        assertFalse(MediaQuery.Update(MediaQuery.Update.Option.Slow).on(100, 100))

        assertTrue(MediaQuery.DisplayMode(MediaQuery.DisplayMode.Option.Standalone).on(100, 100))
        assertFalse(MediaQuery.DisplayMode(MediaQuery.DisplayMode.Option.Browser).on(100, 100))
    }

    @Test
    fun deviceFeaturesIgnoreTheWindowSize() {
        // Device traits are constants; resizing must not change them.
        val pointer = MediaQuery.Pointer(MediaQuery.Pointer.Option.Coarse)
        assertEquals(pointer.on(100, 100), pointer.on(2000, 1500))
    }
}
