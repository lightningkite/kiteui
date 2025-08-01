package com.lightningkite.kiteui.models

public data class DragData(
    public val label: String,
    public val typeToData: Map<String, String>
) {
    public constructor(
        label: String,
        mimeType: String,
        data: String,
    ):this(label, mapOf(mimeType to data))
    public val mimeType: String get() = typeToData.keys.firstOrNull() ?: ""
    public val data: String get() = typeToData.values.firstOrNull() ?: ""

    public operator fun get(mimeType: String) = typeToData[mimeType]
}

data class DragEvent(
    public val data: DragData,
    public val xInView: Double,
    public val yInView: Double
) {
    public val types get() = data.typeToData.keys
}
