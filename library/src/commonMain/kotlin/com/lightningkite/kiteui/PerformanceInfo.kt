package com.lightningkite.kiteui

import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

class PerformanceInfo(val key: String, val parent: PerformanceInfo? = null, val immediate: Boolean = false) {
    var sum: Duration = Duration.ZERO
    var count: Int = 0
    val average get() = sum / count.coerceAtLeast(1)
    var tracking = false
    var time = TimeSource.Monotonic.markNow()

    fun pre(): Boolean {
        val t = tracking
        tracking = true
        time = TimeSource.Monotonic.markNow()
        return t
    }
    fun post(t: Boolean) {
        if(t) {
            tracking = false
            plusAssign(time.elapsedNow())
        }
    }

    fun cancel() {

    }

    inline operator fun <T> invoke(crossinline action: ()->T): T {
        val t = pre()
        return try {
            action()
        } finally {
            post(t)
        }
    }

    operator fun plusAssign(measureTime: Duration) {
        sum += measureTime
        count++
        parent?.plusAssign(measureTime)
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

    init {
        all.add(this)
    }

    companion object {
        var display: Boolean = true
        val all = ArrayList<PerformanceInfo>()
        var lastReport = clockMillis()
        fun reportIfNeeded() {
            if(!display) return
            val now = clockMillis()
            if(now - lastReport > 5000) {
                lastReport = now
                println("---PERFORMANCE REPORT---")
                all.sortedByDescending { it.sum }.forEach { it.print() }
                println("---PERFORMANCE REPORT END---")
            }
        }
    }
}