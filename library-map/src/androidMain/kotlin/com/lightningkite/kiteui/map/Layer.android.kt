package com.lightningkite.kiteui.map

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import org.maplibre.android.style.expressions.Expression as NativeExpression
import org.maplibre.android.style.layers.CircleLayer as NativeCircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer as NativeFillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer as NativeFillLayer
import org.maplibre.android.style.layers.HeatmapLayer as NativeHeatmapLayer
import org.maplibre.android.style.layers.Layer as NativeLayer
import org.maplibre.android.style.layers.LayoutPropertyValue
import org.maplibre.android.style.layers.LineLayer as NativeLineLayer
import org.maplibre.android.style.layers.PaintPropertyValue
import org.maplibre.android.style.layers.SymbolLayer as NativeSymbolLayer

private val Layer<*>.nativeLayerOrNull: NativeLayer?
    get() = mapView.map.state.getOrNull()?.style?.getLayer(id.toString())

/** The live layer. Only valid while [Layer.isLive]. */
private val Layer<*>.nativeLayer: NativeLayer get() = nativeLayerOrNull!!

internal actual val Layer<*>.isNativeLayerAdded: Boolean get() = nativeLayerOrNull != null

internal actual fun Layer<*>.writeNativeProperty(key: PropertyKey, value: JsonElement?) {
    when (key.group) {
        PropertyGroup.Paint -> nativeLayer.setProperties(PaintPropertyValue(key.name, value?.toNativeValue()))
        PropertyGroup.Layout -> nativeLayer.setProperties(LayoutPropertyValue(key.name, value?.toNativeValue()))
        PropertyGroup.Filter -> nativeLayer.setFilter(value.toNativeFilter())
    }
}

// MapLibre Native defaults to unbounded zoom ranges.
internal actual fun Layer<*>.setNativeMinZoom(value: Double?) {
    nativeLayer.minZoom = value?.toFloat() ?: Float.NEGATIVE_INFINITY
}

internal actual fun Layer<*>.setNativeMaxZoom(value: Double?) {
    nativeLayer.maxZoom = value?.toFloat() ?: Float.POSITIVE_INFINITY
}

/** Only the layer types that draw features from a source have filters. */
internal fun NativeLayer.setFilter(filter: NativeExpression) = when (this) {
    is NativeCircleLayer -> setFilter(filter)
    is NativeFillLayer -> setFilter(filter)
    is NativeFillExtrusionLayer -> setFilter(filter)
    is NativeHeatmapLayer -> setFilter(filter)
    is NativeLineLayer -> setFilter(filter)
    is NativeSymbolLayer -> setFilter(filter)
    else -> throw IllegalArgumentException("${this::class.simpleName} does not support filters")
}

/** Filters can't be unset on Android, so no filter is one that is always true. */
internal fun JsonElement?.toNativeFilter(): NativeExpression = when (this) {
    null, JsonNull -> NativeExpression.literal(true)
    is JsonPrimitive -> booleanOrNull?.let { NativeExpression.literal(it) } ?: error("Unsupported filter $this")
    else -> NativeExpression.Converter.convert(toString())
}

internal fun JsonElement.toNativeValue(): Any? = when (this) {
    JsonNull -> null
    is JsonArray -> Array(size) { this[it].toNativeValue() }
    is JsonObject -> HashMap(mapValues { it.value.toNativeValue() })
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: double
}
