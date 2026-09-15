package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.UrlCacheStrategy
import com.lightningkite.kiteui.testing.uiTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A [ImageView] with no source to show - either never assigned one, or explicitly set back to
 * `null` - must settle rather than spin forever. `source = null` goes through
 * `info?.copy(sources = listOfNotNull(null))`, which produces a non-null [ImageView.Info] holding
 * an empty list rather than a null [ImageView.Info], so [ImageView.refresh] has to treat "no
 * sources" the same as "no info" for [ImageView.shownInfo] to ever leave `notReady`.
 */
class ImageViewNullSourceTest {

    @Test
    fun aViewWhoseSourceIsNeverAssignedIsNotStuckLoading() {
        lateinit var view: ImageView
        uiTest(content = {
            view = image {}
        }) {
            assertTrue(view.shownInfo.state.ready, "an empty view must not report notReady forever")
            assertNull(view.shownInfo.state.getOrNull())
            assertNull(view.lastRender)
        }
    }

    @Test
    fun clearingTheSourceStopsTheSpinnerInsteadOfSpinningForever() {
        lateinit var view: ImageView
        uiTest(content = {
            view = image {
                source = ImageRemote("https://example.com/picture", UrlCacheStrategy.Full)
            }
        }) {
            view.source = null
            assertTrue(view.shownInfo.state.ready, "clearing the source must resolve shownInfo instead of leaving it notReady")
            assertNull(view.shownInfo.state.getOrNull())
            assertNull(view.lastRender, "no source means no rendered image views")
        }
    }

    @Test
    fun settingDescriptionAloneDoesNotSpinForever() {
        lateinit var view: ImageView
        uiTest(content = {
            view = image {
                description = "a caption with no picture"
            }
        }) {
            assertTrue(view.shownInfo.state.ready)
            assertNull(view.shownInfo.state.getOrNull())
            assertNull(view.lastRender)
        }
    }
}
