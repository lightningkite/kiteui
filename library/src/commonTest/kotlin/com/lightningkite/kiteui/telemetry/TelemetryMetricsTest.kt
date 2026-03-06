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
        val snapshot = counter.snapshotAndReset(IdGenerator.nanosString())
        assertNull(snapshot, "Empty counter should return null snapshot")
    }

    @Test
    fun counterAccumulatesValues() {
        val counter = CounterAggregator("test.counter", emptyList())
        counter.add(3)
        counter.add(7)
        val snapshot = counter.snapshotAndReset(IdGenerator.nanosString())
        assertNotNull(snapshot)
        assertEquals("test.counter", snapshot.name)
        val dp = snapshot.sum!!.dataPoints.single()
        assertEquals(10L, dp.asInt)
    }

    @Test
    fun counterResetsAfterSnapshot() {
        val counter = CounterAggregator("test.counter", emptyList())
        counter.add(5)
        counter.snapshotAndReset(IdGenerator.nanosString())
        val second = counter.snapshotAndReset(IdGenerator.nanosString())
        assertNull(second, "Counter should be zero after snapshot")
    }

    @Test
    fun counterPreservesAttributes() {
        val attrs = listOf(OtlpKeyValue("method", OtlpAnyValue(stringValue = "GET")))
        val counter = CounterAggregator("test.counter", attrs)
        counter.add(1)
        val snapshot = counter.snapshotAndReset(IdGenerator.nanosString())!!
        assertEquals(attrs, snapshot.sum!!.dataPoints.single().attributes)
    }

    @Test
    fun counterUsesDeltaTemporality() {
        val counter = CounterAggregator("test.counter", emptyList())
        counter.add(1)
        val snapshot = counter.snapshotAndReset(IdGenerator.nanosString())!!
        assertEquals(1, snapshot.sum!!.aggregationTemporality, "Should use DELTA temporality")
        assertEquals(true, snapshot.sum!!.isMonotonic)
    }

    // --- HistogramAggregator ---

    @Test
    fun histogramStartsEmpty() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        val snapshot = hist.snapshotAndReset(IdGenerator.nanosString())
        assertNull(snapshot, "Empty histogram should return null snapshot")
    }

    @Test
    fun histogramRecordsBasicStats() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        hist.record(10.0)
        hist.record(20.0)
        hist.record(30.0)
        val snapshot = hist.snapshotAndReset(IdGenerator.nanosString())
        assertNotNull(snapshot)
        val dp = snapshot.histogram!!.dataPoints.single()
        assertEquals(3L, dp.count)
        assertEquals(60.0, dp.sum)
        assertEquals(10.0, dp.min)
        assertEquals(30.0, dp.max)
    }

    @Test
    fun histogramResetsAfterSnapshot() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        hist.record(100.0)
        hist.snapshotAndReset(IdGenerator.nanosString())
        val second = hist.snapshotAndReset(IdGenerator.nanosString())
        assertNull(second, "Histogram should be empty after snapshot")
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
        val snapshot = hist.snapshotAndReset(IdGenerator.nanosString())!!
        val dp = snapshot.histogram!!.dataPoints.single()
        assertEquals(listOf(10.0, 100.0), dp.explicitBounds)
        assertEquals(listOf(2L, 1L, 1L), dp.bucketCounts)
    }

    @Test
    fun histogramUsesDeltaTemporality() {
        val hist = HistogramAggregator("test.hist", "ms", emptyList())
        hist.record(1.0)
        val snapshot = hist.snapshotAndReset(IdGenerator.nanosString())!!
        assertEquals(1, snapshot.histogram!!.aggregationTemporality, "Should use DELTA temporality")
    }
}
