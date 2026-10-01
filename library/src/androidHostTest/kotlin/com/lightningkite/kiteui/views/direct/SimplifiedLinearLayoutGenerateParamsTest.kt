package com.lightningkite.kiteui.views.direct

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [SimplifiedLinearLayout] has to accept a child carrying any [ViewGroup.LayoutParams].
 *
 * `ViewGroup.addView` converts foreign params by calling `generateLayoutParams` whenever
 * `checkLayoutParams` rejects them, which is routine - a view moved between containers keeps the
 * params its previous parent gave it. The vendored copy of AOSP's LinearLayout replaced that
 * general fallback with `TODO()`, so precisely the case the method exists to handle was the one
 * that threw. These go through `addView` rather than calling the (protected) converter directly,
 * so they exercise the path that actually failed.
 */
@RunWith(RobolectricTestRunner::class)
class SimplifiedLinearLayoutGenerateParamsTest {
    private fun newLayout(): SimplifiedLinearLayout =
        SimplifiedLinearLayout(RuntimeEnvironment.getApplication())

    private fun newChild(): View = View(RuntimeEnvironment.getApplication())

    /** The params the layout actually stored for [child], after any conversion. */
    private fun paramsAfterAdd(
        params: ViewGroup.LayoutParams,
        child: View = newChild(),
    ): SimplifiedLinearLayout.LayoutParams {
        newLayout().addView(child, params)
        return child.layoutParams as SimplifiedLinearLayout.LayoutParams
    }

    @Test
    fun plainLayoutParamsAreConvertedRatherThanRejected() {
        val converted = paramsAfterAdd(ViewGroup.LayoutParams(120, 45))

        assertEquals(120, converted.width)
        assertEquals(45, converted.height)
    }

    @Test
    fun paramsFromAnotherContainerAreAccepted() {
        // The realistic route into the fallback: params minted by a different ViewGroup subclass.
        // This is the case that used to throw.
        val converted = paramsAfterAdd(FrameLayout.LayoutParams(70, 30))

        assertEquals(70, converted.width)
        assertEquals(30, converted.height)
    }

    @Test
    fun marginLayoutParamsAreAccepted() {
        // Note this layout does NOT extend MarginLayoutParams - margins are expressed through the
        // `useMargins` flag instead - so the only contract here is that the conversion happens at
        // all and the size survives. Asserting on margin fields would be asserting on a feature
        // this layout deliberately does not have.
        val converted = paramsAfterAdd(ViewGroup.MarginLayoutParams(80, 20))

        assertEquals(80, converted.width)
        assertEquals(20, converted.height)
    }

    @Test
    fun ourOwnParamsAreLeftAloneRatherThanConverted() {
        val source = SimplifiedLinearLayout.LayoutParams(10, 15).apply {
            weight = 3f
            gravity = Gravity.CENTER
        }
        val child = newChild()
        newLayout().addView(child, source)

        // checkLayoutParams accepts these, so they should be stored as-is. Identity is the point:
        // if a future edit routed them through the copy path instead, weight and gravity would
        // still match but the instance would differ.
        assertTrue(child.layoutParams === source)
        assertEquals(3f, source.weight)
        assertEquals(Gravity.CENTER, source.gravity)
    }

    /**
     * A [UseMarginsLayoutParams] that is deliberately NOT a [SimplifiedLinearLayout.LayoutParams].
     *
     * Needed because [SimplifiedLinearLayout.LayoutParams] is the only concrete subclass in this
     * repo, and it is accepted by `checkLayoutParams` - so handing one back would take the
     * pass-through path and never reach the conversion this test is about. The base class is
     * public and abstract, so a consumer outside this repo can produce exactly this shape.
     */
    private class ForeignUseMarginsParams(width: Int, height: Int) :
        UseMarginsLayoutParams(width, height)

    @Test
    fun theUseMarginsFlagSurvivesConversion() {
        // The one piece of state the general fallback constructor goes out of its way to copy, so
        // it is the part most likely to be silently dropped by a careless rewrite of that branch.
        val source = ForeignUseMarginsParams(10, 10).apply { useMargins = true }

        val converted = paramsAfterAdd(source)

        assertTrue(converted.useMargins, "useMargins was dropped when converting foreign params")
    }

    @Test
    fun theUseMarginsFlagDefaultsToFalseForParamsThatDoNotCarryIt() {
        // Pins the other half of that `as?` in the fallback constructor: params with no opinion
        // must not inherit a stale true.
        assertTrue(!paramsAfterAdd(ViewGroup.LayoutParams(10, 10)).useMargins)
    }
}
