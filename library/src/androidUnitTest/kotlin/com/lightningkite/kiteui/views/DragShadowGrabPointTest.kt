package com.lightningkite.kiteui.views

import android.graphics.Point
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.reactive.context.ReactiveContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * A dragged row must stay under the finger where it was picked up.
 *
 * Android's stock `View.DragShadowBuilder` reports the *centre* of the view as the touch point, so
 * the moment a drag starts the row jumps to centre itself on the finger, and stays offset by
 * however far from the middle it was grabbed. The custom builder KiteUI already had was only used
 * when a caller supplied an explicit `dragShadow`; the default path - which is what drag-to-reorder
 * uses - fell through to the platform's.
 */
@RunWith(RobolectricTestRunner::class)
class DragShadowGrabPointTest {

    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }
        lateinit var row: Element

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            with(viewWriter) {
                col {
                    row = col {
                        // A child that consumes touches, which is the case the first
                        // implementation of this got wrong.
                        text("draggable row")
                        dragData = DragData(label = "row", mimeType = "text/plain", data = "row")
                    }
                }
            }
        }
    }

    /** Sends a gesture start at screen coordinates, the way a real press arrives. */
    private fun touchDownOnScreen(activity: KiteUiActivity, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        activity.dispatchTouchEvent(MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0))
    }

    @Test
    fun theGrabPointIsTakenFromTheGestureEvenWhenAChildConsumesIt() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()
            val element = activity.row.underlyingNativeElement as NativeElement

            touchDownOnScreen(activity, 12f, 34f)

            val grab = assertNotNull(
                element.grabPoint,
                "the gesture must be observable, or the shadow has nothing to anchor to",
            )
            val viewOnScreen = IntArray(2).also(element.native::getLocationOnScreen)
            assertEquals(12 - viewOnScreen[0], grab.x, "the grab point must be in view coordinates")
            assertEquals(34 - viewOnScreen[1], grab.y)
        }
    }

    /**
     * The metrics themselves. This is the assertion that fails on the stock builder: it would
     * report (width/2, height/2) whatever the grab point was.
     */
    @Test
    fun theShadowAnchorsAtTheGrabPointRatherThanTheCentre() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val view = View(controller.get()).apply { layout(0, 0, 200, 80) }

            val size = Point()
            val touch = Point()
            NativeElement.GrabPointShadowBuilder(view, Point(15, 70))
                .onProvideShadowMetrics(size, touch)

            assertEquals(200, size.x, "the shadow must be the size of the view")
            assertEquals(80, size.y)
            assertEquals(15, touch.x, "the shadow must anchor where the row was grabbed, not at 100")
            assertEquals(70, touch.y, "the shadow must anchor where the row was grabbed, not at 40")
        }
    }

    @Test
    fun aGrabPointOutsideTheViewIsClampedIntoIt() {
        // The platform rejects a touch point outside the shadow, so a stale or scaled coordinate
        // has to be brought back in range rather than passed through.
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val view = View(controller.get()).apply { layout(0, 0, 100, 50) }

            val size = Point()
            val touch = Point()
            NativeElement.GrabPointShadowBuilder(view, Point(-20, 999))
                .onProvideShadowMetrics(size, touch)

            assertEquals(0, touch.x)
            assertEquals(50, touch.y)
        }
    }
}
