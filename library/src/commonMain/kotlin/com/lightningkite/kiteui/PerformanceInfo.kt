package com.lightningkite.kiteui

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

public class PerformanceInfo(public val key: String, public val immediate: Boolean = false) {
    public var sum: Duration = Duration.ZERO
    public var count: Int = 0
    public val average: Duration get() = sum / count.coerceAtLeast(1)

    public fun trace(): Trace = Trace()

    public inline operator fun <T> invoke(crossinline action: ()->T): T {
        val time = TimeSource.Monotonic.markNow()
        return try {
            action()
        } finally {
            this@PerformanceInfo += time.elapsedNow()
        }
    }

    public operator fun plusAssign(measureTime: Duration) {
        sum += measureTime
        count++
        if(immediate) {
            print()
        }
        reportIfNeeded()
    }

    public fun reset() {
        sum = Duration.ZERO
        count = 0
    }

    public fun print() {
        println("$key: ${average.inWholeMicroseconds} microseconds (${sum.inWholeMilliseconds}ms / $count)")
        reset()
    }

    public companion object {
        public var display: Boolean = true
        public val all: HashMap<String, PerformanceInfo> = HashMap<String, PerformanceInfo>()
        public var lastReport: Double = clockMillis()
        public fun reportIfNeeded() {
            if(!display) return
            val now = clockMillis()
            if(now - lastReport > 5000) {
                lastReport = now
                all.values.filter { it.count > 0 }.sortedByDescending { it.sum }.forEach { it.print() }
            }
        }
        public operator fun get(key: String): PerformanceInfo = all.getOrPut(key) { PerformanceInfo(key) }
        public fun trace(key: String): Trace = get(key).trace()
    }

    public inner class Trace() {
        public var going: Boolean = true
        public var time: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
        public fun pause() {
            if(!going) throw Exception("Trace mess up")
            going = false
            this@PerformanceInfo.sum += time.elapsedNow()
        }
        public fun resume() {
            if(going) throw Exception("Trace mess up")
            going = true
            time = TimeSource.Monotonic.markNow()
        }
        public fun cancel() {
            if(going) this@PerformanceInfo.sum += time.elapsedNow()
            going = false
            this@PerformanceInfo.count++
        }
    }
}

