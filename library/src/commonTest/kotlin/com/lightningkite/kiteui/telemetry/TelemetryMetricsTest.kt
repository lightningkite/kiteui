// by Claude - tests for CounterAggregator and HistogramAggregator
package com.lightningkite.kiteui.telemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class TelemetryMetricsTest {

    // --- CounterAggregator ---

    @Test
    fun counterStartsAtZero() {
        val counter = CounterAggregator("test.counter", emptyList())
        val snapshot = counter.snapshot(IdGenerator.nanosString())
        assertNull(snapshot, "Empty counter should return null snapshot")
    }

    @Test
    fun counterAccumulatesValues() {
        val counter = CounterAggregator("test.counter", emptyList())
        counter.add(3)
        counter.add(7)
        val snapshot = counter.snapshot(IdGenerator.nanosString())
        assertNotNull(snapshot)
        assertEquals("test.counter", snapshot.name)
        val dp = snapshot.sum!!.dataPoints.single()
        assertEquals(10L, dp.asInt)
    }

    @Test
    fun counterCumulativeKeepsRunningTotal() {
        val counter = CounterAggregator("test.counter", emptyList())
        counter.add(5)
        val first = counter.snapshot(IdGenerator.nanosString())
        assertNotNull(first)
        assertEquals(5L, first.sum!!.dataPoints.single().asInt)
        // Second snapshot should still report the cumulative total
        counter.add(3)
        val second = counter.snapshot(IdGenerator.nanosString())
        assertNotNull(second)
        assertEquals(8L, second.sum!!.dataPoints.single().asInt)
    }

    @Test
    fun counterPreservesAttributes() {
        val attrs = listOf(OtlpKeyValue("method", OtlpAnyValue(stringValue = "GET")))
        val counter = CounterAggregator("test.counter", attrs)
        counter.add(1)
        val snapshot = counter.snapshot(IdGenerator.nanosString())!!
        assertEquals(attrs, snapshot.sum!!.dataPoints.single().attributes)
    }

    @Test
    fun counterUsesCumulativeTemporality() {
        val counter = CounterAggregator("test.counter", emptyList())
        counter.add(1)
        val snapshot = counter.snapshot(IdGenerator.nanosString())!!
        assertEquals(2, snapshot.sum!!.aggregationTemporality, "Should use CUMULATIVE temporality")
        assertEquals(true, snapshot.sum!!.isMonotonic)
    }

    // --- HistogramAggregator ---

    @Test
    fun histogramStartsEmpty() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        val snapshot = hist.snapshot(IdGenerator.nanosString())
        assertNull(snapshot, "Empty histogram should return null snapshot")
    }

    @Test
    fun histogramRecordsBasicStats() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        hist.record(10.0)
        hist.record(20.0)
        hist.record(30.0)
        val snapshot = hist.snapshot(IdGenerator.nanosString())
        assertNotNull(snapshot)
        val dp = snapshot.histogram!!.dataPoints.single()
        assertEquals(3L, dp.count)
        assertEquals(60.0, dp.sum)
        assertEquals(10.0, dp.min)
        assertEquals(30.0, dp.max)
    }

    @Test
    fun histogramCumulativeKeepsRunningTotal() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        hist.record(100.0)
        val first = hist.snapshot(IdGenerator.nanosString())
        assertNotNull(first)
        assertEquals(1L, first.histogram!!.dataPoints.single().count)
        // Second snapshot should include all data
        hist.record(200.0)
        val second = hist.snapshot(IdGenerator.nanosString())
        assertNotNull(second)
        assertEquals(2L, second.histogram!!.dataPoints.single().count)
        assertEquals(300.0, second.histogram!!.dataPoints.single().sum)
    }

    @Test
    fun histogramBucketCountsAreCorrect() {
        // Use simple bounds for easy verification
        val hist = HistogramAggregator("test.hist", "ms", emptyList(), bounds = listOf(10.0, 100.0))
        // Buckets: [<=10], [<=100], [>100]
        hist.record(5.0)    // bucket 0
        hist.record(10.0)   // bucket 0
        hist.record(50.0)   // bucket 1
        hist.record(200.0)  // bucket 2 (overflow)
        val snapshot = hist.snapshot(IdGenerator.nanosString())!!
        val dp = snapshot.histogram!!.dataPoints.single()
        assertEquals(listOf(10.0, 100.0), dp.explicitBounds)
        assertEquals(listOf(2L, 1L, 1L), dp.bucketCounts)
    }

    @Test
    fun histogramUsesCumulativeTemporality() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        hist.record(1.0)
        val snapshot = hist.snapshot(IdGenerator.nanosString())!!
        assertEquals(2, snapshot.histogram!!.aggregationTemporality, "Should use CUMULATIVE temporality")
    }
}
