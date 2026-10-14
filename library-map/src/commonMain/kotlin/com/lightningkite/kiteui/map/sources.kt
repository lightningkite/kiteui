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
import kotlin.jvm.JvmName
import kotlin.uuid.Uuid

class Source<G : Geometry> internal constructor(val mapView: MapView, val geometry: Reactive<G?>) {
    val id = Uuid.random()

    init {
        mapView.sources[id] = buildFeature(geometry = null) { properties = buildJsonObject {  } }
        mapView.reactive {
            mapView.sources.put(id, buildFeature(geometry = geometry()) {
                properties = buildJsonObject { }
            })
        }
    }

    internal fun <L : Layer> addLayer(constructor: (Uuid) -> L, setup: L.() -> Unit) {
        mapView.layers[Uuid.random()] = constructor(id).also(setup)
    }

    internal fun <L : Layer, D> addExpensiveLayer(constructor: (Uuid) -> L, from: Reactive<D>, setup: L.(D) -> Unit) {
        val layerId = Uuid.random()
        mapView.layers[layerId] = null
        mapView.reactive { mapView.layers[layerId] = constructor(id).apply { setup(from()) } }
    }
}

fun <G : Geometry> MapView.source(geometry: Reactive<G?>, setup: Source<G>.() -> Unit = {}) =
    Source(this, geometry).apply(setup)

fun Source<*>.onClick(callback: () -> Unit) {}

fun <G> Source<G>.addFill(setup: FillLayer.() -> Unit) where G : PolygonGeometry = addLayer(::FillLayer, setup)
fun <G, D> Source<G>.addExpensiveFill(from: Reactive<D>, setup: FillLayer.(D) -> Unit)
        where G : PolygonGeometry = addExpensiveLayer(::FillLayer, from, setup)

fun <G> Source<G>.addLine(setup: LineLayer.() -> Unit) where G : LineStringGeometry = addLayer(::LineLayer, setup)
fun <G, D> Source<G>.addExpensiveLine(from: Reactive<D>, setup: LineLayer.(D) -> Unit)
        where G : LineStringGeometry = addExpensiveLayer(::LineLayer, from, setup)

@JvmName("addLine2")
fun <G> Source<G>.addLine(setup: LineLayer.() -> Unit) where G : PolygonGeometry = addLayer(::LineLayer, setup)

@JvmName("addExpensiveLine2")
fun <G, D> Source<G>.addExpensiveLine(from: Reactive<D>, setup: LineLayer.(D) -> Unit)
        where G : PolygonGeometry = addExpensiveLayer(::LineLayer, from, setup)

fun <G> Source<G>.addSymbol(setup: SymbolLayer.() -> Unit) where G : SingleGeometry = addLayer(::SymbolLayer, setup)
fun <G, D> Source<G>.addExpensiveSymbol(from: Reactive<D>, setup: SymbolLayer.(D) -> Unit)
        where G : SingleGeometry = addExpensiveLayer(::SymbolLayer, from, setup)

@JvmName("addSymbol2")
fun <G> Source<G>.addSymbol(setup: SymbolLayer.() -> Unit) where G : MultiGeometry = addLayer(::SymbolLayer, setup)

@JvmName("addExpensiveSymbol2")
fun <G, D> Source<G>.addExpensiveSymbol(from: Reactive<D>, setup: SymbolLayer.(D) -> Unit)
        where G : MultiGeometry = addExpensiveLayer(::SymbolLayer, from, setup)

fun <G> Source<G>.addCircle(setup: CircleLayer.() -> Unit) where G : PointGeometry = addLayer(::CircleLayer, setup)
fun <G, D> Source<G>.addExpensiveCircle(from: Reactive<D>, setup: CircleLayer.(D) -> Unit)
        where G : PointGeometry = addExpensiveLayer(::CircleLayer, from, setup)

fun Source<MultiPoint>.addHeatmap(setup: HeatmapLayer.() -> Unit) = addLayer(::HeatmapLayer, setup)
fun <D> Source<MultiPoint>.addExpensiveHeatmap(from: Reactive<D>, setup: HeatmapLayer.(D) -> Unit) =
    addExpensiveLayer(::HeatmapLayer, from, setup)

fun <G> Source<G>.addFillExtrusion(setup: FillExtrusionLayer.() -> Unit)
        where G : PolygonGeometry = addLayer(::FillExtrusionLayer, setup)

fun <G, D> Source<G>.addExpensiveFillExtrusion(from: Reactive<D>, setup: FillExtrusionLayer.(D) -> Unit)
        where G : PolygonGeometry = addExpensiveLayer(::FillExtrusionLayer, from, setup)

val GeoCoordinate.position get() = Position(longitude, latitude)
val GeoCoordinate.point get() = Point(position)
val Collection<GeoCoordinate>.multiPoint get() = MultiPoint(map { it.position })
val List<GeoCoordinate>.lineString get() = if (size < 2) null else LineString(map { it.position })
val Collection<List<GeoCoordinate>>.multiLineString get() = MultiLineString(mapNotNull { if (size < 2) null else it.map { it.position } })

@get:JvmName("pointN")
val Reactive<GeoCoordinate?>.point get() = lens { it?.point }
val Reactive<GeoCoordinate>.point get() = lens { it.point }

@get:JvmName("multiPointN")
val Reactive<Collection<GeoCoordinate>?>.multiPoint get() = lens { it?.multiPoint }
val Reactive<Collection<GeoCoordinate>>.multiPoint get() = lens { it.multiPoint }

@get:JvmName("lineStringN")
val Reactive<List<GeoCoordinate>?>.lineString get() = lens { it?.lineString }
val Reactive<List<GeoCoordinate>>.lineString get() = lens { it.lineString }

@get:JvmName("multiLineStringN")
val Reactive<Collection<List<GeoCoordinate>>?>.multiLineString get() = lens { it?.multiLineString }
val Reactive<Collection<List<GeoCoordinate>>>.multiLineString get() = lens { it.multiLineString }