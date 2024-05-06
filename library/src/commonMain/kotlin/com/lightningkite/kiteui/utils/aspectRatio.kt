package com.lightningkite.kiteui.utils

import kotlin.js.JsName
import kotlin.jvm.JvmName

@JvmName("fitInsideBoxDouble")
@JsName("fitInsideBoxDouble")
internal fun Pair<Double, Double>.fitInsideBox(width: Double, height: Double): Pair<Double, Double> {
    val aspectWidthToHeight = first / second
    val boxWidthToHeight = width / height

    if (aspectWidthToHeight > boxWidthToHeight) {
        // Width is the limiting dimension
        return width to (width / aspectWidthToHeight)
    } else {
        // Height is the limiting dimension
        return (height * aspectWidthToHeight) to height
    }
}

@JvmName("fitInsideBoxInt")
@JsName("fitInsideBoxInt")
internal fun Pair<Int, Int>.fitInsideBox(width: Double, height: Double) =
    (first.toDouble() to second.toDouble()).fitInsideBox(width, height)