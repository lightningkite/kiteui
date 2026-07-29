package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.views.Element

public data class DragData(
    val label: String,
    val typeToData: Map<String, String>,
    val dragShadow: DragShadow? = null
) {
    public constructor(
        label: String,
        mimeType: String,
        data: String,
        dragShadow: DragShadow? = null
    ) : this(label, mapOf(mimeType to data), dragShadow)

    val mimeType: String get() = typeToData.keys.firstOrNull() ?: ""
    val data: String get() = typeToData.values.firstOrNull() ?: ""

    public operator fun get(mimeType: String) = typeToData[mimeType]
}

public data class DragShadow(
    val view: Element,
    val xAlign: Align = Align.Center,
    val yAlign: Align = Align.Center,
    val xOffset: Dimension? = null,
    val yOffset: Dimension? = null
)

public data class DragEvent(
    val data: DragData,
    val xInView: Double,
    val yInView: Double
) {
    val types get() = data.typeToData.keys
}
