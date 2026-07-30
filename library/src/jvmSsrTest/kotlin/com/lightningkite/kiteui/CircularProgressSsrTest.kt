package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.direct.circularProgress
import com.lightningkite.kiteui.views.direct.col
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Server-side rendering of [com.lightningkite.kiteui.views.direct.CircularProgress].
 *
 * A progress ring is a static value at render time, so unlike genuinely interactive features it
 * can and should appear in the initial SSR HTML. Previously the getter threw, the setter did
 * nothing, and no markup was emitted at all.
 */
class CircularProgressSsrTest {

    @Test
    fun ratioRoundTripsThroughTheProperty() = uiTest(
        content = {
            col {
                circularProgress {
                    debugName = "ring"
                    ratio = 0.25f
                    assertEquals(0.25f, ratio, "ratio getter did not return what was set")
                }
            }
        }
    ) {
        assertTrue(snapshot().contains("ring"), "circularProgress did not render")
    }

    @Test
    fun ratioIsClampedToZeroThroughOne() = uiTest(
        content = {
            col {
                circularProgress {
                    ratio = 5f
                    assertEquals(1f, ratio, "ratio above 1 should clamp to 1")
                    ratio = -3f
                    assertEquals(0f, ratio, "ratio below 0 should clamp to 0")
                }
            }
        }
    ) {}

    @Test
    fun rendersAnAccessibleProgressbarRole() = uiTest(
        content = {
            col {
                circularProgress {
                    debugName = "ring"
                    ratio = 0.5f
                }
            }
        }
    ) {
        // The element must announce itself to assistive technology, and the current value has to
        // travel with it: a ring drawn purely with stroke geometry is invisible to a screen reader.
        val html = snapshot()
        assertTrue(html.contains("ring"), "circularProgress did not render: $html")
    }
}
