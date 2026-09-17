package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.reactive.AppState
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * `AppState.windowInfo` has to keep up with the window.
 *
 * It used to be written exactly once, in the object's initialiser, and never again - there was no
 * rotation or resize observer on iOS at all. Every reader was therefore pinned to whatever the
 * launch orientation happened to be: `shownForQuery` never re-evaluated, `rowCollapsingToColumn`
 * never collapsed, and the media-query screen's own readout sat frozen.
 *
 * The refresh is normally driven by the display link, which a test binary produces no frames for,
 * so it is invoked directly here. Rotation itself cannot be simulated - what is checked is the part
 * that was missing: that something re-reads the platform and republishes when the value differs.
 */
@OptIn(ExperimentalForeignApi::class)
class WindowInfoRefreshTest {

    private fun screenStatistics() = WindowStatistics(
        width = Dimension(UIScreen.mainScreen.bounds.useContents { size.width }),
        height = Dimension(UIScreen.mainScreen.bounds.useContents { size.height }),
        density = UIScreen.mainScreen.scale.toFloat(),
    )

    @Test
    fun refreshingRepublishesTheRealWindowSize() {
        // Stand in for "the window changed while nobody was looking".
        val stale = WindowStatistics(width = Dimension(1.0), height = Dimension(1.0), density = 1f)
        AppState._windowInfo.value = stale
        assertNotEquals(screenStatistics(), stale, "precondition: the stale value differs from reality")

        AppState.refreshWindowInfo()

        assertEquals(
            screenStatistics(),
            AppState.windowInfo.value,
            "a refresh must re-read the platform and republish, not keep the value it was given at launch",
        )
    }

    @Test
    fun refreshingNotifiesListenersExactlyWhenTheValueChanges() {
        AppState.refreshWindowInfo()
        var notifications = 0
        val remove = AppState.windowInfo.addListener { notifications++ }
        try {
            // Already current: nothing to say.
            AppState.refreshWindowInfo()
            assertEquals(0, notifications, "an unchanged window must not wake every reactive reader each frame")

            AppState._windowInfo.value = WindowStatistics(Dimension(1.0), Dimension(1.0), 1f)
            val afterManualSet = notifications
            AppState.refreshWindowInfo()
            assertEquals(
                afterManualSet + 1, notifications,
                "a changed window must notify, or nothing re-evaluates",
            )
        } finally {
            remove()
        }
    }
}
