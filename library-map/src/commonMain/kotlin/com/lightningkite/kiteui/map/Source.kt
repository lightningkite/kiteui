package com.lightningkite.kiteui.map

import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.lensing.lens
import com.lightningkite.services.data.GeoCoordinate
import com.lightningkite.services.database.HasId
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
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
import org.maplibre.spatialk.geojson.GeoJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlin.jvm.JvmName
import kotlin.uuid.Uuid

class Source<G : Geometry?, P : HasId<*>?> internal constructor(
    val mapView: MapView,
    val features: Reactive<Collection<Feature<G, P>>>,
    internal val propertiesSerializer: KSerializer<P>,
) {
    val id = Uuid.random()

    internal var data: String = encode(emptyList())
    internal val clickListeners = ArrayList<(properties: List<P>) -> Unit>()

    init {
        mapView.sources[id] = this
        mapView.reactive {
            data = encode(features().toList())
            mapView.sources[id] = this@Source
        }
    }

    private fun encode(features: List<Feature<G, P>>): String = GeoJson.jsonFormat.encodeToString(
        FeatureCollection.serializer(Geometry.serializer().nullable, propertiesSerializer),
        FeatureCollection(features),
    )

    internal fun <L : Layer<P>> addLayer(constructor: (MapView, Uuid, Uuid) -> L, setup: L.() -> Unit) {
        val layerId = Uuid.random()
        mapView.layers[layerId] = constructor(mapView, layerId, id).also(setup)
    }

    internal fun <L : Layer<P>, D> addExpensiveLayer(
        constructor: (MapView, Uuid, Uuid) -> L,
        from: Reactive<D>,
        setup: L.(D) -> Unit
    ) {
        val layerId = Uuid.random()
        mapView.layers[layerId] = null
        mapView.reactive { mapView.layers[layerId] = constructor(mapView, layerId, id).apply { setup(from()) } }
    }
}

fun <P : HasId<*>> Source<*, P>.onClick(callback: (properties: List<P>) -> Unit) {
    clickListeners += callback
}

fun Source<*, Nothing?>.onClick(callback: () -> Unit) {
    clickListeners += { callback() }
}


fun <G : PolygonGeometry?, P : HasId<*>?> Source<G, P>.addFill(setup: FillLayer<P>.() -> Unit) =
    addLayer(::FillLayer, setup)

fun <G : PolygonGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveFill(
    from: Reactive<D>, setup: FillLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::FillLayer, from, setup)

fun <G : LineStringGeometry?, P : HasId<*>?> Source<G, P>.addLine(setup: LineLayer<P>.() -> Unit) =
    addLayer(::LineLayer, setup)

fun <G : LineStringGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveLine(
    from: Reactive<D>, setup: LineLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::LineLayer, from, setup)

@JvmName("addLine2")
fun <G : PolygonGeometry?, P : HasId<*>?> Source<G, P>.addLine(setup: LineLayer<P>.() -> Unit) =
    addLayer(::LineLayer, setup)

@JvmName("addExpensiveLine2")
fun <G : PolygonGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveLine(
    from: Reactive<D>, setup: LineLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::LineLayer, from, setup)

fun <G : SingleGeometry?, P : HasId<*>?> Source<G, P>.addSymbol(setup: SymbolLayer<P>.() -> Unit) =
    addLayer(::SymbolLayer, setup)

fun <G : SingleGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveSymbol(
    from: Reactive<D>, setup: SymbolLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::SymbolLayer, from, setup)

@JvmName("addSymbol2")
fun <G : MultiGeometry?, P : HasId<*>?> Source<G, P>.addSymbol(setup: SymbolLayer<P>.() -> Unit) =
    addLayer(::SymbolLayer, setup)

@JvmName("addExpensiveSymbol2")
fun <G : MultiGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveSymbol(
    from: Reactive<D>, setup: SymbolLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::SymbolLayer, from, setup)

fun <G : PointGeometry?, P : HasId<*>?> Source<G, P>.addCircle(setup: CircleLayer<P>.() -> Unit) =
    addLayer(::CircleLayer, setup)

fun <G : PointGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveCircle(
    from: Reactive<D>, setup: CircleLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::CircleLayer, from, setup)

fun <G : PointGeometry?, P : HasId<*>?> Source<G, P>.addHeatmap(setup: HeatmapLayer<P>.() -> Unit) =
    addLayer(::HeatmapLayer, setup)

fun <G : PointGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveHeatmap(
    from: Reactive<D>, setup: HeatmapLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::HeatmapLayer, from, setup)

fun <G : PolygonGeometry?, P : HasId<*>?> Source<G, P>.addFillExtrusion(setup: FillExtrusionLayer<P>.() -> Unit) =
    addLayer(::FillExtrusionLayer, setup)

fun <G : PolygonGeometry?, P : HasId<*>?, D> Source<G, P>.addExpensiveFillExtrusion(
    from: Reactive<D>, setup: FillExtrusionLayer<P>.(D) -> Unit,
) = addExpensiveLayer(::FillExtrusionLayer, from, setup)


val GeoCoordinate.position get() = Position(longitude, latitude)
val GeoCoordinate.point get() = Point(position)
val Collection<GeoCoordinate>.multiPoint get() = MultiPoint(map { it.position })
val List<GeoCoordinate>.lineString get() = if (size < 2) null else LineString(map { it.position })
val Collection<List<GeoCoordinate>>.multiLineString get() = MultiLineString(mapNotNull { if (it.size < 2) null else it.map { it.position } })

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


val <T : Geometry?> T.feature get() = Feature<T, Nothing?>(this, null)
val <T : Geometry?> Reactive<T>.feature get() = lens { it.feature }