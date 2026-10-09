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

expect class MapView(context: ElementContext, disableAttribution: I_SWEAR_ON_MY_JOB?, interactive: Boolean) :
    NativeElement, BaseProperties {
    internal val preInit: PreInit

    override var style: Style?
    val camera: Camera

    fun createMarker(position: GeoCoordinate): Marker
}

inline fun ElementWriter.mapView(
    disableAttribution: I_SWEAR_ON_MY_JOB? = null,
    interactive: Boolean = true,
    setup: MapView.() -> Unit,
) = write(MapView(context, disableAttribution, interactive), setup)

expect fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit)


/**
 * Yes, this is meant to be attention grabbing.
 *
 * You really need to handle attribution correctly, as it's potentially a legal issue. Check
 * all the third-party map APIs you are using and what kind of attribution they require, if any.
 * MapLibre itself does not require attribution, as it's licensed under 3-Clause BSD.
 *
 * Setting this doesn't actually do anything behind the scenes. It's just a form of in-code docs
 * (and a way to make it slightly harder to forget to handle it).
 *
 * As an example, basemaps obtained from `Map.Style.Url()` commonly require attribution.
 */
enum class I_SWEAR_ON_MY_JOB {
    /**
     * Choose this if you have checked and verified that none of the third party APIs that you
     * are using require attribution.
     */
    I_DO_NOT_NEED_ATTRIBUTION,

    /** Choose this if you are handling attribution manually elsewhere in the app. */
    I_AM_HANDLING_ATTRIBUTION,
}


    /**
     * The MapLibre map is not initialized immediately on android and web, so we need to keep track
     * of state changes that happen before initialization (e.i., in the KiteUI initializer, which
     * runs first) and forward those to the initializer.
     */
    internal data class PreInit(
        override var style: Style? = Style.Demo,

        // Camera
        override var center: GeoCoordinate = GeoCoordinate(0.0, 0.0),
        override var zoom: Double = 0.0,
        override var minZoom: Double? = null,
        override var maxZoom: Double? = null,
        override var pitch: Double = 0.0,
        override var minPitch: Double? = null,
        override var maxPitch: Double? = null,
    ) : BaseProperties, CameraProperties

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

    sealed interface Style {
        data class Url(val url: String) : Style
        data class Json(val json: String) : Style

        companion object {
            val Demo = Url("https://demotiles.maplibre.org/style.json")
            val Debug = Url("https://demotiles.maplibre.org/debug-tiles/style.json")
            val GlobeDemo = Url("https://demotiles.maplibre.org/globe.json")
        }
    }

    interface BaseProperties {
        var style: Style?
    }

    interface CameraProperties {
        var center: GeoCoordinate
        var zoom: Double
        var minZoom: Double?
        var maxZoom: Double?
        var pitch: Double
        var minPitch: Double?
        var maxPitch: Double?
    }

    interface Camera : CameraProperties {
        fun ease(animateTo: AnimateTo)
        fun fly(animateTo: AnimateTo)
        fun stopAnimation()

        data class AnimateTo(
            val center: GeoCoordinate? = null,
            val zoom: Double? = null,
            val bearing: Double? = null,
            val pitch: Double? = null,
            val duration: Duration,
        )
    }

    interface Feature<T> {
        fun update(data: T)
        fun remove()
    }

    interface Marker : Feature<GeoCoordinate>


/** Creates a reactive map feature */
fun <F : Feature<D>, D : Any> MapView.feature(data: Reactive<D?>, createFeature: MapView.(data: D) -> F) {
    val feature = Signal<F?>(null)

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
fun <ITEM, F : Feature<D>, D : Any> MapView.featureMany(
    items: Reactive<Collection<ITEM>>,
    toId: (it: ITEM) -> Int,
    toData: (item: ITEM) -> D,
    createFeature: MapView.(data: D) -> F
) {
    val features = Signal<Set<Pair<Int, Feature<D>>>>(emptySet())

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


