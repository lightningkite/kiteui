package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.map.style.Layer
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.lensing.lens
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.MultiPoint
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.dsl.buildFeature
import kotlin.uuid.Uuid

fun MapView.feature(source: Reactive<GeoJsonObject>, layers: List<Reactive<Layer>>) {
    val sourceId = Uuid.random()

    reactive { sources.put(sourceId, source()) }

    layers.forEach { layer ->
        val layerId = Uuid.random()

        reactive { this@feature.layers.put(layerId, layer().withSource(sourceId)) }
    }
}

fun MapView.mark(point: Reactive<GeoCoordinate>, layers: List<Reactive<Layer>>) = feature(
    point.lens {
        buildFeature(geometry = Point(it.longitude, it.latitude)) {
            properties = buildJsonObject {  }
        }
    }, layers
)

fun MapView.markAll(points: Reactive<List<GeoCoordinate>>, layers: List<Reactive<Layer>>) = feature(
    points.lens {
        buildFeature(geometry = MultiPoint(coordinates = it.map { Position(it.longitude, it.latitude) })) {
            properties = buildJsonObject {  }
        }
    }, layers
)