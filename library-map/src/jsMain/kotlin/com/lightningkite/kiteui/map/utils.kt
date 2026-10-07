package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.map.maplibre.Listener
import com.lightningkite.kiteui.map.maplibre.LngLat
import com.lightningkite.services.data.GeoCoordinate
import com.lightningkite.kiteui.map.maplibre.Map as MapLibreMap

/**
 * There is a MapLibre bug where null is incorrectly treated differently from undefined
 * for certain fields during initialization. Don't ask how I know...
 */
val JsUndefined = js("undefined")

/** Util for https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/events.ts#L233 */
fun MapLibreMap.onStyleData(callback: () -> Unit) = on("styledata", callback as Listener<Any>)

fun Map.Style.toUnion() = when (this) {
    is Map.Style.Json -> json
    is Map.Style.Url -> url
}

fun LngLat.toGeoCoordinate() = GeoCoordinate(lat, lng)
fun GeoCoordinate.toLngLat() = LngLat(longitude, latitude)