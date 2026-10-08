package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.ReactiveState
import com.lightningkite.reactive.core.Signal
import com.lightningkite.services.data.GeoCoordinate
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.time.Duration

expect class MapView(context: ElementContext) : NativeElement {
    var style: Map.Style?

    internal val preInit: PreInit

    inner class Camera {
        var center: GeoCoordinate
        var zoom: Double
        var minZoom: Double?
        var maxZoom: Double?
        var pitch: Double
        var minPitch: Double?
        var maxPitch: Double?

        fun easeTo(options: Map.EaseToOptions)
        fun flyTo(options: Map.FlyToOptions)
        fun stopAnimation()
    }

    val camera: Camera

    fun createMarker(position: GeoCoordinate): Map.Marker
}

inline fun ElementWriter.mapView(setup: MapView.() -> Unit) =
    write(MapView(context), setup)


object Map {
    sealed interface Style {
        data class Url(val url: String) : Style
        data class Json(val json: String) : Style

        companion object {
            val Demo = Url("https://demotiles.maplibre.org/style.json")
            val Debug = Url("https://demotiles.maplibre.org/debug-tiles/style.json")
            val GlobeDemo = Url("https://demotiles.maplibre.org/globe.json")
        }
    }


    interface CameraOptions {
        val center: GeoCoordinate?
        val zoom: Double?
        val bearing: Double?
        val pitch: Double?
    }

    interface AnimationOptions {
        val duration: Duration?
        val easing: Easing

        sealed interface Easing {
            object Default : Easing
            object Linear : Easing
            data class CubicBezier(val x1: Double, val y1: Double, val x2: Double, val y2: Double) : Easing
        }
    }

    data class EaseToOptions(
        override val center: GeoCoordinate? = null,
        override val zoom: Double? = null,
        override val bearing: Double? = null,
        override val pitch: Double? = null,
        override val duration: Duration,
        override val easing: AnimationOptions.Easing = AnimationOptions.Easing.Default,
    ) : CameraOptions, AnimationOptions

    data class FlyToOptions(
        override val center: GeoCoordinate? = null,
        override val zoom: Double? = null,
        override val bearing: Double? = null,
        override val pitch: Double? = null,
        override val duration: Duration,
        override val easing: AnimationOptions.Easing = AnimationOptions.Easing.Default,
    ) : CameraOptions, AnimationOptions

    interface Feature<T> {
        fun update(data: T)
        fun remove()
    }

    interface Marker : Feature<GeoCoordinate>
}


/** Creates a reactive map feature */
fun <FEATURE : Map.Feature<D>, D : Any> MapView.feature(
    data: Reactive<D?>,
    createFeature: MapView.(data: D) -> FEATURE
) {
    val feature = Signal<FEATURE?>(null)

    reactive(reentrancyLimit = 1) {
        val data = data()
        if (data == null) {
            feature()?.remove()
            feature.value = null
            return@reactive
        }

        feature()?.update(data)
        if (feature() == null) feature.value = createFeature(data)
    }
}

/** Creates a reactive set of map features */
fun <ITEM, FEATURE : Map.Feature<D>, D : Any> MapView.featureMany(
    items: Reactive<Collection<ITEM>>,
    toId: (it: ITEM) -> Int,
    toData: (item: ITEM) -> D,
    createFeature: MapView.(data: D) -> FEATURE
) {
    val features = Signal<Set<Pair<Int, Map.Feature<D>>>>(emptySet())

    reactive(reentrancyLimit = 1) {
        val items = items()
        val currentMarkers = features()

        val newPoints = items.filter { !currentMarkers.map { it.first }.contains(toId(it)) }
        val newMarkers = newPoints.map { toId(it) to createFeature(toData(it)) }

        val removeMarkers = currentMarkers.filter { !items.map(toId).contains(it.first) }
        removeMarkers.forEach { it.second.remove() }

        features.value = currentMarkers.filter { items.map(toId).contains(it.first) }.toSet() + newMarkers
    }
}

fun MapView.mark(data: Reactive<GeoCoordinate?>) = feature(data) { createMarker(it) }
fun <T> MapView.markMany(items: Reactive<Collection<T>>, toId: (it: T) -> Int, toData: (it: T) -> GeoCoordinate) =
    featureMany(items, toId, toData) { createMarker(it) }


/**
 * The MapLibre map is not initialized immediately on android and web, so we need to keep track
 * of state changes that happen before initialization (e.i., in the KiteUI initializer, which
 * runs first) and forward those to the initializer.
 */
internal data class PreInit(
    var style: Map.Style? = Map.Style.Demo,
    var center: GeoCoordinate = GeoCoordinate(0.0, 0.0),
    var zoom: Double = 0.0,
    var minZoom: Double? = null,
    var maxZoom: Double? = null,
    var pitch: Double = 0.0,
    var minPitch: Double? = null,
    var maxPitch: Double? = null,
)

internal class LateInit<T, M>(
    val mapView: MapView,
    val getMapState: MapView.() -> ReactiveState<M>,
    val preInitProp: KMutableProperty1<PreInit, T>,
    val getter: M.() -> T,
    val setter: M.(T) -> Unit,
) : ReadWriteProperty<Any, T> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
        mapView.getMapState().getOrNull()?.run { getter() } ?: preInitProp.get(mapView.preInit)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        preInitProp.set(mapView.preInit, value)
        mapView.getMapState().getOrNull()?.setter(value)
    }
}
