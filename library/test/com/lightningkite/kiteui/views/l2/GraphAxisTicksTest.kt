package com.lightningkite.kiteui.views.l2

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [axisTicks], the index arithmetic behind a graph's tick marks and labels.
 *
 * The arithmetic is easy to get wrong in a way that is invisible until a specific label count, and
 * wrong in a way that silently erases an entire axis rather than crashing. `from`/`to` pad the
 * plotted range by 5% on each side, so with `n` labels and `step = range / (n - 1)` the padding is
 * `0.05 * (n - 1)` steps — under one step up to 20 labels, a whole step at 21, two at 41. That means
 * a labelled axis legitimately produces positions both **below index 0** and **above the last
 * index**, and the two need opposite treatment: skip the low ones, stop at the high ones.
 *
 * Indexing the list with whatever the arithmetic produced crashed. Breaking out of the loop on any
 * out-of-range index instead dropped every tick on an axis with 21+ labels, because the very first
 * position is the negative one.
 */
class GraphAxisTicksTest {

    /** Mirrors GraphDelegate: labels span rawMin..rawMax, and the drawn range pads that by 5%. */
    private fun ticksForLabels(labels: List<String>, rawMin: Double = 0.0, rawMax: Double = 100.0): List<Pair<Double, String?>> {
        val range = rawMax - rawMin
        return axisTicks(
            from = rawMin - range * 0.05,
            to = rawMax + range * 0.05,
            step = range / (labels.size - 1),
            labelOrigin = rawMin,
            labels = labels,
        )
    }

    @Test
    fun everyLabelGetsExactlyOneTick() {
        // 21 is the first count where the 5% padding reaches a full step, and the count at which
        // breaking on any out-of-range index wiped the axis out entirely.
        for (count in listOf(2, 7, 20, 21, 25, 41, 100)) {
            val labels = (0 until count).map { "L$it" }
            val ticks = ticksForLabels(labels)
            assertEquals(
                labels,
                ticks.map { it.second },
                "wrong labels for a $count-label axis",
            )
        }
    }

    @Test
    fun labelsAreAnchoredToTheRangeMinimumNotZero() {
        // Data extending below zero moves the origin; indexing from 0 would shift every label.
        val labels = listOf("a", "b", "c", "d", "e")
        val ticks = ticksForLabels(labels, rawMin = -40.0, rawMax = 0.0)
        assertEquals(labels, ticks.map { it.second })
        assertEquals(-40.0, ticks.first().first, 1e-9)
        assertEquals(0.0, ticks.last().first, 1e-9)
    }

    @Test
    fun unlabelledAxisTicksAtRoundMultiplesOfStepFromZero() {
        val ticks = axisTicks(from = -1.0, to = 10.0, step = 5.0, labelOrigin = -1.0, labels = null)
        assertEquals(listOf(0.0, 5.0, 10.0), ticks.map { it.first })
        assertTrue(ticks.all { it.second == null }, "an unlabelled axis must leave formatting to the caller")
    }

    @Test
    fun degenerateStepProducesNoTicksRatherThanLooping() {
        // The reachable one: `xAxisLabels = emptyList()` makes step = range / (0 - 1), i.e. negative,
        // and the old loop then walked away from `to` forever.
        assertEquals(emptyList(), axisTicks(0.0, 10.0, -10.0, 0.0, emptyList()))
        // A single label makes step = range / 0 = Infinity, and an empty graph can reach 0.0.
        assertEquals(emptyList(), axisTicks(0.0, 10.0, 0.0, 0.0, null))
        assertEquals(emptyList(), axisTicks(0.0, 10.0, Double.POSITIVE_INFINITY, 0.0, listOf("only")))
        assertEquals(emptyList(), axisTicks(0.0, 10.0, Double.NaN, 0.0, null))
    }
}
