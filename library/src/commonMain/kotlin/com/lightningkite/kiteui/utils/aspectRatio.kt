package com.lightningkite.kiteui.utils

fun Pair<Double, Double>.fitInsideBox(width: Double, height: Double): Pair<Double, Double> {
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

fun Pair<Int, Int>.fitInsideBox(width: Double, height: Double) =
    (first.toDouble() to second.toDouble()).fitInsideBox(width, height)