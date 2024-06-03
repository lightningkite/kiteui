package com.lightningkite.kiteui

import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.measureTime

class PerformanceInfo(val key: String, val parent: PerformanceInfo? = null) {
    var sum: Duration = Duration.ZERO
    var count: Int = 0
    val average get() = sum / count
    inline operator fun <T> invoke(crossinline action: ()->T): T {
        val result: T
        this += measureTime {
            result = action()
        }
        return result
    }

    var lastReport = clockMillis()
    operator fun plusAssign(measureTime: Duration) {
        sum += measureTime
        count++
        parent?.plusAssign(measureTime)
        val now = clockMillis()
        if(now - lastReport > 5000) {
            lastReport = now
            println("$key: ${average.inWholeMicroseconds} microseconds (${sum.inWholeMilliseconds}ms / $count)")
            reset()
        }
    }

    fun reset() {
        sum = Duration.ZERO
        count = 0
    }

    companion object {
        val calcSizes = PerformanceInfo("calcSizes")
        val linearCalcSizes = PerformanceInfo("linearCalcSizes", calcSizes)
        val frameCalcSizes = PerformanceInfo("frameCalcSizes", calcSizes)
        val layout = PerformanceInfo("layout")
        val linearLayout = PerformanceInfo("linearLayout", layout)
        val frameLayout = PerformanceInfo("frameLayout", layout)
        val frameLayoutPart = PerformanceInfo("frameLayoutPart", null)
        val measure = PerformanceInfo("measure")
        val linearMeasure = PerformanceInfo("linearMeasure", measure)
        val frameMeasure = PerformanceInfo("frameMeasure", measure)
    }
}