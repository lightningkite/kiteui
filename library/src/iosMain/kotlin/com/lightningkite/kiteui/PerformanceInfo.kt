package com.lightningkite.kiteui

import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.measureTime

class PerformanceInfo(val key: String, val parent: PerformanceInfo? = null) {
    var sum: Duration = Duration.ZERO
    var count: Int = 0
    val average get() = sum / count.coerceAtLeast(1)
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
        val all = ArrayList<PerformanceInfo>()
        var lastReport = clockMillis()
        fun reportIfNeeded() {
            val now = clockMillis()
            if(now - lastReport > 5000) {
                lastReport = now
                all.forEach { it.print() }
            }
        }

        val calcSizes = PerformanceInfo("calcSizes")
        val linearCalcSizes = PerformanceInfo("linearCalcSizes", calcSizes)
        val frameCalcSizes = PerformanceInfo("frameCalcSizes", calcSizes)
        val layout = PerformanceInfo("layout")
        val linearLayout = PerformanceInfo("linearLayout", layout)
        val frameLayout = PerformanceInfo("frameLayout", layout)
        val measure = PerformanceInfo("measure")
        val linearMeasure = PerformanceInfo("linearMeasure", measure)
        val frameMeasure = PerformanceInfo("frameMeasure", measure)
    }
}