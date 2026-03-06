// by Claude - client-side metric aggregation: counters and histograms
// Aggregated locally to minimize export volume; flushed as OTLP delta temporality.
package com.lightningkite.kiteui.telemetry

/**
 * Aggregates a monotonically increasing counter.
 * Exported as an OTLP Sum with delta temporality — each flush reports the delta since last flush.
 */
internal class CounterAggregator(
    val name: String,
    val attributes: List<OtlpKeyValue>,
) {
    private var value: Long = 0L
    private var startNanos: String = IdGenerator.nanosString()

    fun add(delta: Long = 1) {
        value += delta
    }

    /** Returns the OTLP metric and resets the counter for the next interval. Null if no data. */
    fun snapshotAndReset(nowNanos: String): OtlpMetric? {
        if (value == 0L) return null
        val snapshot = OtlpMetric(
            name = name,
            sum = OtlpSum(
                dataPoints = listOf(
                    OtlpNumberDataPoint(
                        startTimeUnixNano = startNanos,
                        timeUnixNano = nowNanos,
                        asInt = value,
                        attributes = attributes,
                    )
                ),
                aggregationTemporality = 1, // DELTA
                isMonotonic = true,
            )
        )
        value = 0L
        startNanos = nowNanos
        return snapshot
    }
}

/**
 * Aggregates values into an explicit-bucket histogram.
 * Default bucket boundaries are tuned for HTTP latency in milliseconds.
 * Exported as OTLP Histogram with delta temporality.
 */
internal class HistogramAggregator(
    val name: String,
    val unit: String,
    val attributes: List<OtlpKeyValue>,
    val bounds: List<Double> = DEFAULT_LATENCY_BOUNDS,
) {
    companion object {
        // by Claude - bucket boundaries tuned for typical HTTP latency (ms)
        val DEFAULT_LATENCY_BOUNDS = listOf(
            0.0, 5.0, 10.0, 25.0, 50.0, 75.0, 100.0,
            250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0
        )
    }

    private var count: Long = 0L
    private var sum: Double = 0.0
    private var min: Double = Double.MAX_VALUE
    private var max: Double = -Double.MAX_VALUE
    private var bucketCounts = LongArray(bounds.size + 1) // +1 for overflow bucket
    private var startNanos: String = IdGenerator.nanosString()

    fun record(value: Double) {
        count++
        sum += value
        if (value < min) min = value
        if (value > max) max = value
        val idx = bounds.indexOfFirst { value <= it }.let { if (it == -1) bounds.size else it }
        bucketCounts[idx]++
    }

    /** Returns the OTLP metric and resets for the next interval. Null if no data. */
    fun snapshotAndReset(nowNanos: String): OtlpMetric? {
        if (count == 0L) return null
        val snapshot = OtlpMetric(
            name = name,
            unit = unit,
            histogram = OtlpHistogram(
                dataPoints = listOf(
                    OtlpHistogramDataPoint(
                        startTimeUnixNano = startNanos,
                        timeUnixNano = nowNanos,
                        count = count,
                        sum = sum,
                        min = min,
                        max = max,
                        bucketCounts = bucketCounts.toList(),
                        explicitBounds = bounds,
                        attributes = attributes,
                    )
                ),
                aggregationTemporality = 1, // DELTA
            )
        )
        count = 0L
        sum = 0.0
        min = Double.MAX_VALUE
        max = -Double.MAX_VALUE
        bucketCounts = LongArray(bounds.size + 1)
        startNanos = nowNanos
        return snapshot
    }
}
