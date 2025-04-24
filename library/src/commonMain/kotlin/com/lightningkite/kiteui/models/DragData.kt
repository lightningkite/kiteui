package com.lightningkite.kiteui.models

data class DragData(
    val label: String,
    val typeToData: Map<String, String>
) {
    constructor(
        label: String,
        mimeType: String,
        data: String,
    ):this(label, mapOf(mimeType to data))
    val mimeType: String get() = typeToData.keys.firstOrNull() ?: ""
    val data: String get() = typeToData.values.firstOrNull() ?: ""
}

data class DragEvent(
    val data: DragData,
    val xInView: Double,
    val yInView: Double
)
