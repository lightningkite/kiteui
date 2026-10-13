package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.map.style.CircleLayer
import com.lightningkite.kiteui.map.style.FillExtrusionLayer
import com.lightningkite.kiteui.map.style.FillLayer
import com.lightningkite.kiteui.map.style.HeatmapLayer
import com.lightningkite.kiteui.map.style.Layer
import com.lightningkite.kiteui.map.style.LineLayer
import com.lightningkite.kiteui.map.style.SymbolLayer
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.lensing.lens
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.LineStringGeometry
import org.maplibre.spatialk.geojson.MultiGeometry
import org.maplibre.spatialk.geojson.MultiLineString
import org.maplibre.spatialk.geojson.MultiPoint
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.PointGeometry
import org.maplibre.spatialk.geojson.PolygonGeometry
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.SingleGeometry
import org.maplibre.spatialk.geojson.dsl.buildFeature
import kotlin.uuid.Uuid

class Source<G : Geometry> internal constructor(val mapView: MapView, val geometry: Reactive<G?>) {
    val id = Uuid.random()

    init {
        mapView.reactive {
            mapView.sources.put(id, buildFeature(geometry = geometry()) {
                properties = buildJsonObject { }
            })
        }
    }

    internal fun <L : Layer> layer(constructor: (Uuid) -> L, setup: L.() -> Unit) {
        mapView.layers[Uuid.random()] = constructor(id).also(setup)
    }

    internal fun <L : Layer, D> layer(constructor: (Uuid) -> L, from: Reactive<D>, setup: L.(D) -> Unit) {
        val layerId = Uuid.random()
//        mapView.layers[layerId] = null
        mapView.reactive { mapView.layers[layerId] = constructor(id).apply { setup(from()) } }
    }
}

fun <G : Geometry> MapView.source(geometry: Reactive<G?>, setup: Source<G>.() -> Unit = {}) =
    Source(this, geometry).apply(setup)

fun Source<*>.onClick(callback: () -> Unit) {}

fun <G> Source<G>.fill(setup: FillLayer.() -> Unit) where G : PolygonGeometry = layer(::FillLayer, setup)
fun <G, D> Source<G>.fill(from: Reactive<D>, setup: FillLayer.(D) -> Unit) where G : PolygonGeometry =
    layer(::FillLayer, from, setup)

fun <G> Source<G>.line(setup: LineLayer.() -> Unit) where G : LineStringGeometry = layer(::LineLayer, setup)
fun <G, D> Source<G>.line(from: Reactive<D>, setup: LineLayer.(D) -> Unit)
        where G : LineStringGeometry = layer(::LineLayer, from, setup)

fun <G> Source<G>.line(setup: LineLayer.() -> Unit) where G : PolygonGeometry = layer(::LineLayer, setup)
fun <G, D> Source<G>.line(from: Reactive<D>, setup: LineLayer.(D) -> Unit)
        where G : PolygonGeometry = layer(::LineLayer, from, setup)

fun <G> Source<G>.symbol(setup: SymbolLayer.() -> Unit) where G : SingleGeometry = layer(::SymbolLayer, setup)
fun <G, D> Source<G>.symbol(from: Reactive<D>, setup: SymbolLayer.(D) -> Unit)
        where G : SingleGeometry = layer(::SymbolLayer, from, setup)

fun <G> Source<G>.symbol(setup: SymbolLayer.() -> Unit) where G : MultiGeometry = layer(::SymbolLayer, setup)
fun <G, D> Source<G>.symbol(from: Reactive<D>, setup: SymbolLayer.(D) -> Unit)
        where G : MultiGeometry = layer(::SymbolLayer, from, setup)

fun <G> Source<G>.circle(setup: CircleLayer.() -> Unit) where G : PointGeometry = layer(::CircleLayer, setup)
fun <G, D> Source<G>.circle(from: Reactive<D>, setup: CircleLayer.(D) -> Unit)
        where G : PointGeometry = layer(::CircleLayer, from, setup)

fun <G> Source<G>.heatmap(setup: HeatmapLayer.() -> Unit)
        where G : PointGeometry, G : MultiGeometry = layer(::HeatmapLayer, setup)

fun <G, D> Source<G>.heatmap(from: Reactive<D>, setup: HeatmapLayer.(D) -> Unit)
        where G : PointGeometry, G : MultiGeometry = layer(::HeatmapLayer, from, setup)

fun <G> Source<G>.fillExtrusion(setup: FillExtrusionLayer.() -> Unit)
        where G : PolygonGeometry = layer(::FillExtrusionLayer, setup)

fun <G, D> Source<G>.fillExtrusion(from: Reactive<D>, setup: FillExtrusionLayer.(D) -> Unit)
        where G : PolygonGeometry = layer(::FillExtrusionLayer, from, setup)

val GeoCoordinate.position get() = Position(longitude, latitude)
val GeoCoordinate.point get() = Point(position)
val Collection<GeoCoordinate>.multiPoint get() = MultiPoint(map { it.position })
val List<GeoCoordinate>.lineString get() = if (size < 2) null else LineString(map { it.position })
val Collection<List<GeoCoordinate>>.multiLineString get() = MultiLineString(mapNotNull { if (size < 2) null else it.map { it.position } })

val Reactive<GeoCoordinate>.point get() = lens { it.point }
val Reactive<GeoCoordinate?>.point get() = lens { it?.point }
val Reactive<Collection<GeoCoordinate>>.multiPoint get() = lens { it.multiPoint }
val Reactive<Collection<GeoCoordinate>?>.multiPoint get() = lens { it?.multiPoint }
val Reactive<List<GeoCoordinate>>.lineString get() = lens { it.lineString }
val Reactive<List<GeoCoordinate>?>.lineString get() = lens { it?.lineString }
val Reactive<Collection<List<GeoCoordinate>>>.multiLineString get() = lens { it.multiLineString }
val Reactive<Collection<List<GeoCoordinate>>?>.multiLineString get() = lens { it?.multiLineString }