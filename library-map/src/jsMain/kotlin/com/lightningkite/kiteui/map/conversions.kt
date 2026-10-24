package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.dom.DOMElement
import com.lightningkite.kiteui.map.maplibre.Listener
import com.lightningkite.kiteui.map.maplibre.LngLat
import com.lightningkite.kiteui.map.maplibre.PaddingOptions
import com.lightningkite.services.data.GeoCoordinate
import com.lightningkite.kiteui.map.maplibre.Map as MapLibreMap

/**
 * There is a MapLibre bug where null is incorrectly treated differently from undefined
 * for certain fields during initialization. Don't ask how I know...
 */
internal val JsUndefined = js("undefined")

internal fun PreInit.toOptions(
    container: DOMElement,
    disableAttribution: I_SWEAR_ON_MY_JOB?,
    interactive: Boolean,
): MapLibreMap.Options {
    return MapLibreMap.Options(
        container = container,
        interactive = interactive,
        style = style.toUnion(),

        center = center.toLngLat(),
        zoom = zoom,
        minZoom = minZoom ?: JsUndefined,
        maxZoom = maxZoom ?: JsUndefined,
        bearing = bearing,
        pitch = pitch,
        minPitch = minPitch ?: JsUndefined,
        maxPitch = maxPitch ?: JsUndefined,
    ).also { if (disableAttribution != null) it.attributionControl = false }
}

/** Util for https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/events.ts#L233 */
internal fun MapLibreMap.onStyleData(callback: () -> Unit) = on("styledata", callback as Listener<Any>)

internal fun Style.toUnion() = when (this) {
    is Style.Json -> json
    is Style.Url -> url
}

internal fun geoCoordinateFromLngLat(lngLat: LngLat) = GeoCoordinate(lngLat.lat, lngLat.lng)
internal fun LngLat.toGeoCoordinate() = GeoCoordinate(lat, lng)
internal fun GeoCoordinate.toLngLat() = LngLat(longitude, latitude)

internal fun Camera.Padding.toPaddingOptions() = PaddingOptions(
    top = top ?: JsUndefined,
    bottom = bottom ?: JsUndefined,
    right = right ?: JsUndefined,
    left = left ?: JsUndefined,
)

internal fun PaddingOptions.toPadding() = Camera.Padding(
    top = top ?: 0.0,
    bottom = bottom ?: 0.0,
    right = right ?: 0.0,
    left = left ?: 0.0,
)
