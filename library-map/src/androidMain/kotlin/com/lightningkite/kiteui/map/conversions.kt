package com.lightningkite.kiteui.map

import com.lightningkite.services.data.GeoCoordinate
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style as MapLibreStyle

internal fun PreInit.toOptions(map: MapLibreMap, onStyleLoaded: (MapLibreStyle) -> Unit) {
    map.setStyle(style.toBuilder(), onStyleLoaded)
    map.cameraPosition = CameraPosition.Builder()
        .target(center.toLatLng())
        .zoom(zoom)
        .bearing(bearing)
        .tilt(pitch)
        .padding(padding.toDoubleArray())
        .build()
    minZoom?.let { map.setMinZoomPreference(it) }
    maxZoom?.let { map.setMaxZoomPreference(it) }
    minPitch?.let { map.setMinPitchPreference(it) }
    maxPitch?.let { map.setMaxPitchPreference(it) }
}

internal fun MapLibreStyle?.toMapStyle(): Style {
    if (this == null) return Style.Json("{}")
    return if (uri.isEmpty()) Style.Json(json) else Style.Url(uri)
}

internal fun Style?.toBuilder(): MapLibreStyle.Builder = when (this) {
    is Style.Json -> MapLibreStyle.Builder().fromJson(json)
    is Style.Url -> MapLibreStyle.Builder().fromUri(url)
    null -> MapLibreStyle.Builder().fromJson("""{"version":8,"sources":{},"layers":[]}""")
}

internal fun GeoCoordinate.toLatLng() = LatLng(latitude, longitude)
internal fun LatLng.toGeoCoordinate() = GeoCoordinate(latitude, longitude)

internal fun Camera.Padding.toDoubleArray(): DoubleArray = doubleArrayOf(left, top, right, bottom)
internal fun DoubleArray.toPadding() = Camera.Padding(
    left = this[0],
    top = this[1],
    right = this[2],
    bottom = this[3],
)

internal fun Camera.AnimateTo.toCameraUpdate(map: MapLibreMap): CameraUpdate = CameraUpdateFactory.newCameraPosition(
    CameraPosition.Builder(map.cameraPosition).apply {
        center?.let { target(it.toLatLng()) }
        zoom?.let { zoom(it) }
        bearing?.let { bearing(it) }
        padding?.let { padding(it.toDoubleArray()) }
        pitch?.let { tilt(it) }
    }.build()
)
