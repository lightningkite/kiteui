package com.lightningkite.kiteui.views.canvas

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.direct.CanvasDelegate
import com.lightningkite.kiteui.views.direct.canvas
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.reactive.context.ReactiveContext
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.lightningkite.kiteui.views.direct.Canvas as KiteUiCanvas

/**
 * The half of RTL canvas alignment that [TextAlignRtlTest] deliberately does not cover: whether the
 * view's resolved layout direction actually reaches the drawing context.
 *
 * `TextAlignRtlTest` constructs [DrawingContext2DImpl] with the flag set by hand, so it proves the
 * start/end mapping and nothing about where the flag comes from. That plumbing - `layoutDirection ==
 * View.LAYOUT_DIRECTION_RTL` inside `NCanvas.onDraw` - is the part that can silently do nothing: a
 * view whose direction never resolves reports LTR forever, and every alignment would quietly keep
 * its left-to-right meaning with no error anywhere.
 *
 * Everything goes through [KiteUiActivity] rather than a bare `NCanvas`, because constructing a
 * [CanvasDelegate] at all touches `Theme.placeholder`, which reads the density values
 * `AndroidAppContext` only populates once a KiteUI activity has started.
 */
@RunWith(RobolectricTestRunner::class)
class CanvasLayoutDirectionTest {

    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator { Routes(listOf(), mapOf(), Page.Empty) }
        override val theme: ReactiveContext.() -> Theme = { Theme(id = "unitTest") }
        lateinit var canvasElement: KiteUiCanvas

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            with(viewWriter) {
                frame { canvasElement = canvas { } }
            }
        }
    }

    /**
     * Puts the whole environment into [rtl], draws the canvas, and hands the drawing context to
     * [read].
     *
     * The direction is set with a Robolectric qualifier, which is what Developer Options' "Force
     * RTL" does on a device: it changes the Configuration, and the Configuration is what the canvas
     * reads. Assigning `view.layoutDirection` would not work here - that setter records only a
     * *requested* direction, and the getter returns a resolved one that no amount of measuring a
     * view in isolation will produce (confirmed empirically: under a `+ldrtl` qualifier the
     * Configuration reports RTL while every View in the tree, decor view included, still reports
     * LTR). That gap is the reason the canvas reads the Configuration at all.
     */
    private fun <T> whileDrawing(rtl: Boolean, read: (DrawingContext2DImpl) -> T): T {
        RuntimeEnvironment.setQualifiers(if (rtl) "+ldrtl" else "+ldltr")
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            val native = controller.get().canvasElement.native

            native.measure(
                View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            )
            native.layout(0, 0, 200, 100)

            var result: T? = null
            var drew = false
            native.delegate = object : CanvasDelegate() {
                override fun draw(context: DrawingContext2D) {
                    drew = true
                    result = read(context as DrawingContext2DImpl)
                }
            }

            val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
            native.draw(android.graphics.Canvas(bitmap))

            check(drew) { "the delegate was never asked to draw, so nothing was verified" }
            @Suppress("UNCHECKED_CAST")
            return result as T
        }
    }

    private fun alignFor(rtl: Boolean, align: TextAlign): android.graphics.Paint.Align =
        whileDrawing(rtl) { context ->
            context.textAlign(align)
            context.fillPaintObj.textAlign
        }

    @Test
    fun anRtlViewReportsRtlToTheDrawingContext() {
        assertTrue(
            whileDrawing(rtl = true) { it.isRtl },
            "a canvas laid out RTL must tell the drawing context so, or start/end never swap",
        )
    }

    @Test
    fun anLtrViewReportsLtrToTheDrawingContext() {
        assertEquals(
            false,
            whileDrawing(rtl = false) { it.isRtl },
            "an LTR canvas must not claim RTL",
        )
    }

    @Test
    fun startAndEndFollowTheLayoutDirection() {
        assertEquals(
            android.graphics.Paint.Align.LEFT,
            alignFor(rtl = false, TextAlign.start),
            "start must anchor left under LTR",
        )
        assertEquals(
            android.graphics.Paint.Align.RIGHT,
            alignFor(rtl = true, TextAlign.start),
            "start must anchor right under RTL",
        )
        assertEquals(
            android.graphics.Paint.Align.RIGHT,
            alignFor(rtl = false, TextAlign.end),
            "end must anchor right under LTR",
        )
        assertEquals(
            android.graphics.Paint.Align.LEFT,
            alignFor(rtl = true, TextAlign.end),
            "end must anchor left under RTL",
        )
    }

    /**
     * The non-relative variants must be immune to the setting. This is the pair a reader of the
     * verification screen is most likely to believe has moved, because LEFT-aligned text is drawn
     * to the *right* of its anchor - so the word "left" appearing on the right of the marker is
     * correct, in both directions.
     */
    @Test
    fun explicitLeftAndRightNeverMove() {
        assertEquals(
            android.graphics.Paint.Align.LEFT,
            alignFor(rtl = true, TextAlign.left),
            "explicit left must stay left under RTL",
        )
        assertEquals(
            android.graphics.Paint.Align.RIGHT,
            alignFor(rtl = true, TextAlign.right),
            "explicit right must stay right under RTL",
        )
        assertEquals(
            android.graphics.Paint.Align.LEFT,
            alignFor(rtl = false, TextAlign.left),
            "explicit left must stay left under LTR",
        )
        assertEquals(
            android.graphics.Paint.Align.RIGHT,
            alignFor(rtl = false, TextAlign.right),
            "explicit right must stay right under LTR",
        )
    }
}
