package com.lightningkite.kiteui.map

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.ColorReliefLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.HillshadeLayer
import org.maplibre.android.style.expressions.Expression as MapLibreExpression
import org.maplibre.android.style.layers.CircleLayer as MapLibreCircleLayer
import org.maplibre.android.style.layers.FillExtrusionLayer as MapLibreFillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer as MapLibreFillLayer
import org.maplibre.android.style.layers.HeatmapLayer as MapLibreHeatmapLayer
import org.maplibre.android.style.layers.Layer as MapLibreLayer
import org.maplibre.android.style.layers.LayoutPropertyValue
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.LineLayer as MapLibreLineLayer
import org.maplibre.android.style.layers.PaintPropertyValue
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.SymbolLayer as MapLibreSymbolLayer

private val Layer<*>.nativeLayerOrNull: MapLibreLayer?
    get() = mapView.map.state.getOrNull()?.style?.getLayer(id.toString())

/** The live layer. Only valid while [Layer.isLive]. */
private val Layer<*>.nativeLayer: MapLibreLayer get() = nativeLayerOrNull!!

internal actual val Layer<*>.isNativeLayerAdded: Boolean get() = nativeLayerOrNull != null

internal actual fun Layer<*>.writeLiveProperty(prop: Layer<*>.Property<*>, value: JsonElement?) {
    when (prop.group) {
        Layer.PropertyGroup.Paint -> nativeLayer.setProperties(PaintPropertyValue(prop.name, value?.toNativeValue()))
        Layer.PropertyGroup.Layout -> nativeLayer.setProperties(LayoutPropertyValue(prop.name, value?.toNativeValue()))
        Layer.PropertyGroup.Filter -> nativeLayer.setFilter(value.toNativeFilter())
    }
}

internal actual fun Layer<*>.setLiveMinZoom(value: Double?) {
    nativeLayer.minZoom = value?.toFloat() ?: Float.NEGATIVE_INFINITY
}

internal actual fun Layer<*>.setLiveMaxZoom(value: Double?) {
    nativeLayer.maxZoom = value?.toFloat() ?: Float.POSITIVE_INFINITY
}

/** Only the layer types that draw features from a source have filters. */
internal fun MapLibreLayer.setFilter(filter: MapLibreExpression) = when (this) {
    is MapLibreCircleLayer -> setFilter(filter)
    is MapLibreFillLayer -> setFilter(filter)
    is MapLibreFillExtrusionLayer -> setFilter(filter)
    is MapLibreHeatmapLayer -> setFilter(filter)
    is MapLibreLineLayer -> setFilter(filter)
    is MapLibreSymbolLayer -> setFilter(filter)
    else -> throw IllegalArgumentException("${this::class.simpleName} does not support filters")
}

/** Filters can't be unset on Android, so no filter is one that is always true. */
internal fun JsonElement?.toNativeFilter(): MapLibreExpression = when (this) {
    null, JsonNull -> MapLibreExpression.literal(true)
    is JsonPrimitive -> booleanOrNull?.let { MapLibreExpression.literal(it) } ?: error("Unsupported filter $this")
    else -> MapLibreExpression.Converter.convert(toString())
}

internal fun JsonElement.toNativeValue(): Any? = when (this) {
    JsonNull -> null
    is JsonArray -> Array(size) { this[it].toNativeValue() }
    is JsonObject -> HashMap(mapValues { it.value.toNativeValue() })
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: double
}


internal fun Layer<*>.layerFromJson(id: String): MapLibreLayer {
    val json = toJson()
    val type = json["type"]?.jsonPrimitive?.content
    val source = json["source"]?.jsonPrimitive?.content ?: ""
    val layer = when (type) {
        "background" -> BackgroundLayer(id)
        "circle" -> CircleLayer(id, source)
        "color-relief" -> ColorReliefLayer(id, source)
        "fill" -> FillLayer(id, source)
        "fill-extrusion" -> FillExtrusionLayer(id, source)
        "heatmap" -> HeatmapLayer(id, source)
        "hillshade" -> HillshadeLayer(id, source)
        "line" -> LineLayer(id, source)
        "raster" -> RasterLayer(id, source)
        "symbol" -> SymbolLayer(id, source)
        else -> throw IllegalArgumentException("Unsupported layer type '$type'")
    }

    json["minzoom"]?.let { layer.minZoom = it.jsonPrimitive.float }
    json["maxzoom"]?.let { layer.maxZoom = it.jsonPrimitive.float }

    json["filter"]?.let { layer.setFilter(it.toNativeFilter()) }

    val properties = ArrayList<PropertyValue<*>>()
    json["paint"]?.jsonObject?.forEach { (name, value) ->
        properties += PaintPropertyValue(
            name,
            value.toNativeValue()
        )
    }
    json["layout"]?.jsonObject?.forEach { (name, value) ->
        properties += LayoutPropertyValue(
            name,
            value.toNativeValue()
        )
    }
    layer.setProperties(*properties.toTypedArray())

    return layer
}

