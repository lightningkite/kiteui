package com.lightningkite.kiteui.telemetry

/**
 * Aggregates a monotonically increasing counter.
 * Exported as an OTLP Sum with cumulative temporality — each flush reports the running total.
 */
internal class CounterAggregator(
    val name: String,
    val attributes: List<OtlpKeyValue>,
) {
    private var value: Long = 0L
    private val startNanos: String = Telemetry.nanosString()

    fun add(delta: Long = 1) {
        value += delta
    }

    /** Returns the OTLP metric with cumulative total. Null if no data recorded yet. */
    fun snapshot(nowNanos: String): OtlpMetric? {
        if (value == 0L) return null
        return OtlpMetric(
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
                aggregationTemporality = 2, // CUMULATIVE
                isMonotonic = true,
            )
        )
    }
}

/**
 * Aggregates values into an explicit-bucket histogram.
 * Default bucket boundaries are tuned for HTTP latency in milliseconds.
 * Exported as OTLP Histogram with cumulative temporality.
 */
internal class HistogramAggregator(
    val name: String,
    val unit: String,
    val attributes: List<OtlpKeyValue>,
    val bounds: List<Double> = DEFAULT_LATENCY_BOUNDS,
) {
    companion object {
        /** Exclusive upper bounds in milliseconds for HTTP latency buckets.
         *  A value is placed in the first bucket whose bound is >= the value. */
        val DEFAULT_LATENCY_BOUNDS = listOf(
            5.0, 10.0, 25.0, 50.0, 75.0, 100.0,
            250.0, 500.0, 1000.0, 2500.0, 5000.0, 10000.0
        )
    }

    private var count: Long = 0L
    private var sum: Double = 0.0
    private var min: Double = Double.MAX_VALUE
    private var max: Double = -Double.MAX_VALUE
    private var bucketCounts = LongArray(bounds.size + 1) // +1 for overflow bucket
    private val startNanos: String = Telemetry.nanosString()

    fun record(value: Double) {
        count++
        sum += value
        if (value < min) min = value
        if (value > max) max = value
        val idx = bounds.indexOfFirst { value <= it }.let { if (it == -1) bounds.size else it }
        bucketCounts[idx]++
    }

    /** Returns the OTLP metric with cumulative totals. Null if no data recorded yet. */
    fun snapshot(nowNanos: String): OtlpMetric? {
        if (count == 0L) return null
        return OtlpMetric(
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
                aggregationTemporality = 2, // CUMULATIVE
            )
        )
    }
}
