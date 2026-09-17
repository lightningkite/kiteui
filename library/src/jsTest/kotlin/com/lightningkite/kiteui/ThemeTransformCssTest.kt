package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.Transformation
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.themed
import org.w3c.dom.HTMLElement
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A theme transform reaching the browser as one valid `transform` declaration.
 *
 * `translate3d()` takes exactly three arguments. The emitter used to append only the non-zero ones,
 * so a theme translating in x and y produced `translate3d(20px, 10px)` - invalid, which makes the
 * parser drop the *entire* declaration, silently taking the rotation and scale with it. The
 * symptom was a transform that appeared to apply only one of its components, or none.
 *
 * Asserting on the composed matrix rather than the declaration text is deliberate: it is the only
 * form that shows every component survived, and it is what the eye is actually judging.
 */
class ThemeTransformCssTest {

    private object CompositeTransform : Semantic("compositeTransformCssTest") {
        override fun default(theme: Theme): ThemeAndBack = theme.withBack(
            transform = Transformation(
                translationX = 20.0,
                translationY = 10.0,
                rotation = 20.0,
                scaleX = 1.4,
                scaleY = 1.4,
            )
        )
    }

    private fun mountTransformed(): HTMLElement {
        lateinit var element: Element
        root(Theme(id = "unitTest")) {
            col {
                themed(CompositeTransform).col {
                    element = this
                    text("transformed")
                }
            }
        }
        return element.native.element as HTMLElement
    }

    /** `matrix(a, b, c, d, e, f)` -> the six numbers, or null if the browser reported no transform. */
    private fun matrixOf(el: HTMLElement): List<Double>? {
        val computed = kotlinx.browser.window.getComputedStyle(el).transform
        if (!computed.startsWith("matrix(")) return null
        return computed.removePrefix("matrix(").removeSuffix(")").split(",").map { it.trim().toDouble() }
    }

    @Test
    fun theTransformReachesTheBrowserAtAll() {
        val el = mountTransformed()
        val computed = kotlinx.browser.window.getComputedStyle(el).transform

        assertTrue(
            computed != "none" && computed.isNotBlank(),
            "the whole transform declaration was dropped; computed transform was '$computed'",
        )
    }

    @Test
    fun everyComponentSurvivesIntoTheComposedMatrix() {
        val el = mountTransformed()
        val m = matrixOf(el) ?: throw AssertionError("expected a matrix, got no transform at all")

        // translate3d(20,10,0) rotate(20deg) scale(1.4) composes to
        // matrix(cos20*1.4, sin20*1.4, -sin20*1.4, cos20*1.4, 20, 10).
        val cos20 = 0.93969
        val sin20 = 0.34202
        fun close(actual: Double, expected: Double, what: String) = assertTrue(
            abs(actual - expected) < 0.01,
            "$what: expected ~$expected, got $actual (full matrix $m)",
        )

        close(m[0], cos20 * 1.4, "scale+rotation (a)")
        close(m[1], sin20 * 1.4, "rotation (b)")
        close(m[2], -sin20 * 1.4, "rotation (c)")
        close(m[3], cos20 * 1.4, "scale+rotation (d)")
        close(m[4], 20.0, "x translation (e)")
        close(m[5], 10.0, "y translation (f)")
    }

    @Test
    fun theTranslationIsNotSilentlyDroppedWhileTheRestApplies() {
        // The specific shape of the old failure worth naming: rotation and scale are expressed by
        // a, b, c, d and translation by e and f, so a matrix that is right in the first four and
        // zero in the last two means the invalid translate3d took only itself out.
        val m = matrixOf(mountTransformed()) ?: throw AssertionError("no transform at all")

        assertTrue(
            m[4] != 0.0 || m[5] != 0.0,
            "rotation and scale applied but translation was dropped: $m",
        )
    }
}
