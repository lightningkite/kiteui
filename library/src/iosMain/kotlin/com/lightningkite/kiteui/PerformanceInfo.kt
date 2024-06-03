package com.lightningkite.kiteui

import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.measureTime

private val _calcSizes = PerformanceInfo("calcSizes")
val PerformanceInfo.Companion.calcSizes get() = _calcSizes
private val _linearCalcSizes = PerformanceInfo("linearCalcSizes", _calcSizes)
val PerformanceInfo.Companion.linearCalcSizes get() = _linearCalcSizes
private val _frameCalcSizes = PerformanceInfo("frameCalcSizes", _calcSizes)
val PerformanceInfo.Companion.frameCalcSizes get() = _frameCalcSizes
private val _layout = PerformanceInfo("layout")
val PerformanceInfo.Companion.layout get() = _layout
private val _linearLayout = PerformanceInfo("linearLayout", _layout)
val PerformanceInfo.Companion.linearLayout get() = _linearLayout
private val _frameLayout = PerformanceInfo("frameLayout", _layout)
val PerformanceInfo.Companion.frameLayout get() = _frameLayout
private val _measure = PerformanceInfo("measure")
val PerformanceInfo.Companion.measure get() = _measure
private val _linearMeasure = PerformanceInfo("linearMeasure", _measure)
val PerformanceInfo.Companion.linearMeasure get() = _linearMeasure
private val _frameMeasure = PerformanceInfo("frameMeasure", _measure)
val PerformanceInfo.Companion.frameMeasure get() = _frameMeasure