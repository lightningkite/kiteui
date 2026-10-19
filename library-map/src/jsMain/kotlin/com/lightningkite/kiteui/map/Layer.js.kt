package com.lightningkite.kiteui.map

import kotlinx.serialization.json.JsonElement
import com.lightningkite.kiteui.map.maplibre.Map as MapLibreMap

/** The live map. Only valid while [Layer.isLive]. */
private val Layer<*>.map: MapLibreMap get() = mapView.map.state.getOrNull()!!

internal actual val Layer<*>.isNativeLayerAdded: Boolean
    get() = mapView.isStyleLoaded && mapView.map.state.getOrNull()?.getLayer(id.toString()) != null

internal actual fun Layer<*>.writeNativeProperty(key: PropertyKey, value: JsonElement?) {
    val json = value?.let { JSON.parse<Any?>(it.toString()) }
    when (key.group) {
        PropertyGroup.Paint -> map.setPaintProperty(id.toString(), key.name, json)
        PropertyGroup.Layout -> map.setLayoutProperty(id.toString(), key.name, json)
        PropertyGroup.Filter -> map.setFilter(id.toString(), json)
    }
}

internal actual fun Layer<*>.setNativeMinZoom(value: Double?) {
    map.setLayerZoomRange(id.toString(), value ?: MIN_ZOOM, maxZoom ?: MAX_ZOOM)
}

internal actual fun Layer<*>.setNativeMaxZoom(value: Double?) {
    map.setLayerZoomRange(id.toString(), minZoom ?: MIN_ZOOM, value ?: MAX_ZOOM)
}

/** The style spec's bounds for `minzoom` and `maxzoom`, which are their defaults. */
private const val MIN_ZOOM = 0.0
private const val MAX_ZOOM = 24.0
