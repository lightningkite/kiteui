package com.lightningkite.kiteui

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

class PerformanceInfo(val key: String, val immediate: Boolean = false) {
    var sum: Duration = Duration.ZERO
    var count: Int = 0
    val average get() = sum / count.coerceAtLeast(1)

    fun trace() = Trace()

    inline operator fun <T> invoke(crossinline action: ()->T): T {
        val time = TimeSource.Monotonic.markNow()
        return try {
            action()
        } finally {
            this@PerformanceInfo += time.elapsedNow()
        }
    }

    operator fun plusAssign(measureTime: Duration) {
        sum += measureTime
        count++
        if(immediate) {
            print()
        }
        reportIfNeeded()
    }

    fun reset() {
        sum = Duration.ZERO
        count = 0
    }

    fun print() {
        println("$key: ${average.inWholeMicroseconds} microseconds (${sum.inWholeMilliseconds}ms / $count)")
        reset()
    }

    companion object {
        var display: Boolean = true
        val all = HashMap<String, PerformanceInfo>()
        var lastReport = clockMillis()
        fun reportIfNeeded() {
            if(!display) return
            val now = clockMillis()
            if(now - lastReport > 5000) {
                lastReport = now
                all.values.filter { it.count > 0 }.sortedByDescending { it.sum }.forEach { it.print() }
            }
        }
        operator fun get(key: String) = all.getOrPut(key) { PerformanceInfo(key) }
        fun trace(key: String) = get(key).trace()
    }

    inner class Trace() {
        var going = true
        var time = TimeSource.Monotonic.markNow()
        fun pause() {
            if(!going) throw Exception("Trace mess up")
            going = false
            this@PerformanceInfo.sum += time.elapsedNow()
        }
        fun resume() {
            if(going) throw Exception("Trace mess up")
            going = true
            time = TimeSource.Monotonic.markNow()
        }
        fun cancel() {
            if(going) this@PerformanceInfo.sum += time.elapsedNow()
            going = false
            this@PerformanceInfo.count++
        }
    }
}

