package com.lightningkite.kiteui.models

data class DragData(
    val label: String,
    val mimeType: String,
    val data: String
)

data class DragEvent(
    val data: DragData,
    val xInView: Double,
    val yInView: Double
)
