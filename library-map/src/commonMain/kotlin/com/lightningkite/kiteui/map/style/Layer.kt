package com.lightningkite.kiteui.map.style

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.uuid.Uuid

/**
 * A MapLibre layer, without a source. The implementations ([CircleLayer], [FillLayer], [LineLayer],
 * [SymbolLayer]) are generated from the MapLibre style spec by the `generateMapLibreStyle` task.
 *
 * See https://maplibre.org/maplibre-style-spec/layers/
 */
sealed interface Layer {
    /** Only features matching this are drawn. Zoom is only evaluated at integer levels, and `featureState` isn't allowed. */
    val filter: Expression<Boolean>?

    /** The layer is hidden below this zoom level. */
    val minZoom: Double?

    /** The layer is hidden at and above this zoom level. */
    val maxZoom: Double?

    /** This layer drawing [source], a key of `MapView.sources`. */
    fun withSource(source: Uuid): LayerWithSource
}

/**
 * A [Layer] with the source it draws, as `MapView.layers` holds. The implementations
 * ([CircleLayerWithSource], etc.) are generated alongside the [Layer]s.
 */
sealed interface LayerWithSource {
    /** The id of the source this layer draws, as a key of `MapView.sources`. */
    val source: Uuid

    /** See [Layer.filter]. */
    val filter: Expression<Boolean>?

    /** See [Layer.minZoom]. */
    val minZoom: Double?

    /** See [Layer.maxZoom]. */
    val maxZoom: Double?

    /** This layer without its source. */
    fun withoutSource(): Layer

    /** The style spec JSON for this layer, without its `id`. */
    fun toJson(): JsonObject
}

internal fun LayerWithSource.layerJson(
    type: String,
    paint: Map<String, Expression<*>?>,
    layout: Map<String, Expression<*>?>,
): JsonObject = buildJsonObject {
    put("type", type)
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
data class Offset(val x: Double, val y: Double)

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
