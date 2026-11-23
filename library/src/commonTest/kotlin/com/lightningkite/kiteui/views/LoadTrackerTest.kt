package com.lightningkite.kiteui.views

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoadTrackerTest {
    private val LoadTracker.isLoading get() = !state.ready

    @Test
    fun loadsUntilEverythingThatStartedHasFinished() {
        val tracker = LoadTracker(outside = null)
        tracker.startedLoading()
        tracker.startedLoading()
        tracker.finishedLoading()
        assertTrue(tracker.isLoading)
        tracker.finishedLoading()
        assertFalse(tracker.isLoading)
    }

    @Test
    fun aTrackerTellsTheOneOutsideOnlyWhenItStartsAndStopsLoading() {
        var outsideChanges = 0
        val outside = LoadTracker(outside = null)
        val release = outside.addListener { outsideChanges++ }
        val inside = LoadTracker(outside)
        repeat(3) { inside.startedLoading() }
        assertTrue(outside.isLoading)
        repeat(3) { inside.finishedLoading() }
        assertFalse(outside.isLoading)
        assertEquals(2, outsideChanges)
        release()
    }
}
