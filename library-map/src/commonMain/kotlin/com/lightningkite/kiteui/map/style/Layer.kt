package com.lightningkite.kiteui.map.style

import com.lightningkite.services.data.kabobCase
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.uuid.Uuid

sealed interface Layer {
    val source: Uuid
    var filter: Expression<Boolean>?
    var minZoom: Number?
    var maxZoom: Number?

    fun toJson(): JsonObject
}

fun Layer.onClick(callback: () -> Unit) {

}

internal fun Layer.layerJson(
    paint: Map<String, Expression<*>?>,
    layout: Map<String, Expression<*>?>,
): JsonObject = buildJsonObject {
    put("type", this@layerJson::class.simpleName?.removeSuffix("Layer")?.kabobCase())
    put("source", source.toString())
    filter?.let { put("filter", it.json) }
    minZoom?.let { put("minzoom", it) }
    maxZoom?.let { put("maxzoom", it) }
    put("paint", JsonObject(paint.mapNotNull { (key, value) -> value?.let { key to it.json } }.toMap()))
    put("layout", JsonObject(layout.mapNotNull { (key, value) -> value?.let { key to it.json } }.toMap()))
}

/** Implemented by the generated enums of style spec values. */
interface StyleEnum {
    /** The value as written in the style spec. */
    val value: String
}

/** An `[x, y]` offset. Positive values are right and down. */
data class Point(val x: Double, val y: Double)

/** Padding in the style spec's `[top, right, bottom, left]` order. */
data class Padding(val top: Double, val right: Double, val bottom: Double, val left: Double) {
    constructor(all: Double) : this(all, all, all, all)
    constructor(vertical: Double, horizontal: Double) : this(vertical, horizontal, vertical, horizontal)
}

/**
 * Text that may contain sections with different fonts, scales, and colors. Create with [format], or
 * convert a string expression with [formatted].
 */
sealed interface Formatted

/** An image from the style's sprite or added to the map. Create with [image]. */
sealed interface ResolvedImage

internal val StyleEnum.json get() = JsonPrimitive(value)
