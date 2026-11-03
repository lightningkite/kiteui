package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks in the [LinearChildHost] / [WeightedLayoutParams] capability interfaces that
 * `weight()`/`align()` in modifiers.android.kt use (via `parent?.native as? LinearChildHost`)
 * instead of casting to the concrete [SimplifiedLinearLayout] class. The modifiers no longer throw
 * when the parent doesn't support weight/align - they silently no-op - so this test asserts the
 * capability check itself resolves correctly for both a supporting and a non-supporting parent.
 */
@RunWith(RobolectricTestRunner::class)
class LinearChildHostTest {
    @Test
    fun simplifiedLinearLayout_isLinearChildHost_reportsItsOrientation() {
        val row = SimplifiedLinearLayout(RuntimeEnvironment.getApplication()).apply {
            orientation = SimplifiedLinearLayout.HORIZONTAL
        }
        val col = SimplifiedLinearLayout(RuntimeEnvironment.getApplication()).apply {
            orientation = SimplifiedLinearLayout.VERTICAL
        }

        assertTrue((row as LinearChildHost).isHorizontal)
        assertFalse((col as LinearChildHost).isHorizontal)
    }

    @Test
    fun simplifiedLinearLayoutLayoutParams_isWeightedLayoutParams() {
        val lp = SimplifiedLinearLayout.LayoutParams(0, 0)
        val weighted = lp as WeightedLayoutParams
        weighted.weight = 2f
        assertEquals(2f, lp.weight)
    }

    @Test
    fun nonLinearParent_isNotALinearChildHost_soWeightAndAlignModifiersNoOp() {
        // FrameLayout has no orientation concept; a weight/align modifier applied to a child of one
        // must degrade to a no-op (not throw) - this is what `parent?.native as? LinearChildHost`
        // resolving to null lets the modifier code detect.
        val frame = FrameLayout(RuntimeEnvironment.getApplication())
        assertNull(frame as? LinearChildHost)
    }
}
