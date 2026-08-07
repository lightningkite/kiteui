package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.testing.BaseUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the two things about [ImageVector.kiteUiLogo] that are easy to break by nudging a
 * coordinate: the mark has to stay inside its view box on every platform, and it has to take the
 * color it is handed on every path.
 */
class KiteUiLogoTest : BaseUiTest() {

    /** Every coordinate in the mark's `M x,y L x,y ... Z` paths. */
    private fun coordinates(vector: ImageVector): List<Pair<Double, Double>> =
        vector.paths.flatMap { path ->
            path.path.split('M', 'L', 'Z')
                .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                .map { point ->
                    val (x, y) = point.split(',')
                    x.trim().toDouble() to y.trim().toDouble()
                }
        }

    @Test
    fun staysInsideTheViewBox() {
        val logo = ImageVector.kiteUiLogo()
        // A stroked vertex is drawn at least half a stroke width out from its coordinate, so
        // anything nearer than that to an edge is definitely clipped. Miter joins reach further
        // still - how much further depends on the angle - so this catches obvious breakage rather
        // than proving the drawn mark fits; the rendered mark is the real check for that.
        val margin = logo.paths.maxOf { it.strokeWidth ?: 0.0 } / 2
        for ((x, y) in coordinates(logo)) {
            assertTrue(
                x >= margin && x <= logo.viewBoxWidth - margin &&
                        y >= margin && y <= logo.viewBoxHeight - margin,
                "($x, $y) is too close to the edge of the ${logo.viewBoxWidth}x${logo.viewBoxHeight} view box",
            )
        }
    }

    @Test
    fun takesTheColorItIsGiven() {
        val logo = ImageVector.kiteUiLogo(Color.white)
        for (path in logo.paths) {
            assertTrue(
                path.fillColor == Color.white || path.strokeColor == Color.white,
                "path ${path.path.take(20)} kept a color other than the one it was given",
            )
        }
    }

    @Test
    fun sizeAppliesToBothAxes() {
        val logo = ImageVector.kiteUiLogo(size = 6.rem)
        assertEquals(6.rem, logo.width)
        assertEquals(6.rem, logo.height)
    }
}
