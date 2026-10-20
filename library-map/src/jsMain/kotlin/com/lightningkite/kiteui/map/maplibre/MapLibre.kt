/**
 * External MapLibre TypeScript typedefs
 *
 * If the npm package version changes, this may become out-of-sync! Double check that the
 * current version of each linked source file is the same as the npm package version in
 * /library-map/build.gradle.kts before using this as the source of truth.
 *
 * If these versions are out-of-sync, please update them to match. It should just take a
 * find-and-replace to update the tagged version. The referenced line numbers will take
 * more effort though, so don't worry too much about those. It should be easy enough to
 * find the typedef as long as the file it is defined in hasn't changed.
 *
 * If you are working on this and notice a link that is incorrect AND you have the correct
 * reference on hand, please update it!
 *
 * Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0
 */
@file:JsModule("maplibre-gl") @file:JsNonModule

package com.lightningkite.kiteui.map.maplibre

import kotlinx.js.JsPlainObject
import org.w3c.dom.Element
import kotlin.js.Promise

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L592 */
external class Map(options: Options) {
    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1041 */
    fun getCenter(): LngLat

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1054 */
    fun setCenter(center: LngLat, eventData: Any? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1116 */
    fun getZoom(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1130 */
    fun setZoom(zoom: Double, eventData: Any? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1308 */
    fun getPitch(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1317 */
    fun setPitch(pitch: Double, eventData: Any? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1480 */
    fun easeTo(options: EaseToOptions, eventData: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1517 */
    fun flyTo(options: FlyToOptions, eventData: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1521 */
    fun stop(): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1807 */
    fun getMinZoom(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1774 */
    fun setMinZoom(minZoom: Double? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1858 */
    fun getMaxZoom(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1827 */
    fun setMaxZoom(maxZoom: Double? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1902 */
    fun getMinPitch(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1871 */
    fun setMinPitch(minPitch: Double? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1946 */
    fun getMaxPitch(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1915 */
    fun setMaxPitch(maxPitch: Double? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2338 */
    fun on(type: String, listener: Listener<Any>): Subscription

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2680 */
    fun setStyle(style: Any?, options: Any? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2822 */
    fun getStyle(): StyleSpecification

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2838 */
    fun getStyleUrl(): String?

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2867 */
    fun isStyleLoaded(): Boolean

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2912 */
    fun addSource(id: String, source: SourceSpecification): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2931 */
    fun isSourceLoaded(id: String): Boolean

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3066 */
    fun removeSource(id: String): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3093 */
    fun getSource(id: String): Source?

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3189 */
    fun addImage(id: String, image: Any, options: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3356 */
    fun hasImage(id: String): Boolean

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3379 */
    fun removeImage(id: String)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3502 */
    fun addLayer(layer: AddLayerObject, beforeId: String? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3520 */
    fun moveLayer(id: String, beforeId: String? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3538 */
    fun removeLayer(id: String): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3557 */
    fun getLayer(id: String): StyleLayer?

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3596 */
    fun setLayerZoomRange(layerId: String, minzoom: Double, maxzoom: Double): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3634 */
    fun setFilter(layerId: String, filter: Any?, options: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3665 */
    fun setPaintProperty(layerId: String, name: String, value: Any?, options: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L3693 */
    fun setLayoutProperty(layerId: String, name: String, value: Any?, options: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L84 */
    @JsPlainObject
    interface Options {
        val container: Element
        val style: Any?
        val interactive: Boolean?
        var attributionControl: Any?

        val center: LngLat?
        val zoom: Double?
        val minZoom: Double?
        val maxZoom: Double?
        val pitch: Double?
        val minPitch: Double?
        val maxPitch: Double?

        val fadeDuration: Double?
    }
}


/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/source/source.ts#L31 */
external interface Source

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/style/style_layer.ts */
external interface StyleLayer

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/source/geojson_source.ts#L165 */
abstract external class GeoJSONSource : Source {
    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/source/geojson_source.ts#L297 */
    fun setData(data: Any)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/source/geojson_source.ts#L317 */
    fun updateData(diff: Diff): Promise<Unit>

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/source/geojson_source.ts#L334 */
    fun getData(): Promise<Any>

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/source/geojson_source_diff.ts#L10 */
    @JsPlainObject
    interface Diff {
        val removeAll: Boolean?
        val remove: List<String>?
        val add: List<Any>?
        val update: List<FeatureDiff>?

        /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/source/geojson_source_diff.ts#L33 */
        @JsPlainObject
        interface FeatureDiff {
            val id: String
            val newGeometry: Any?
            val removeAllProperties: Boolean?
            val removeProperties: List<String>?
            val addOrUpdateProperties: List<KeyValuePair>

            @JsPlainObject
            interface KeyValuePair {
                val key: String
                val value: Any
            }
        }
    }
}

/**
 * Generated from reference defined at https://github.com/maplibre/maplibre-style-spec/blob/v26.4.4/src/reference/v8.json
 * Docs: https://maplibre.org/maplibre-style-spec/
 */
@JsPlainObject
external interface StyleSpecification

/**
 * Generated from reference defined at https://github.com/maplibre/maplibre-style-spec/blob/v26.4.4/src/reference/v8.json
 * Docs: https://maplibre.org/maplibre-style-spec/sources/
 */
@JsPlainObject
external interface SourceSpecification

/**
 * Generated from reference defined at https://github.com/maplibre/maplibre-style-spec/blob/v26.4.4/src/reference/v8.json
 * Docs: https://maplibre.org/maplibre-style-spec/layers/
 */
@JsPlainObject
external interface LayerSpecification

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/style/style.ts#L199 */
@JsPlainObject
external interface AddLayerObject : LayerSpecification

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/marker.ts#L288 */
external class Marker(options: Options? = definedExternally) {
    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/marker.ts#L391 */
    fun addTo(map: Map): Marker

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/marker.ts#L428 */
    fun remove(): Marker

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/marker.ts#L490 */
    fun setLngLat(lngLat: LngLat): Marker

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/marker.ts#L104 */
    @JsPlainObject
    interface Options
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/camera.ts#L232 */
@JsPlainObject
external interface AnimationOptions {
    val duration: Double?
    val easing: ((_: Double) -> Double)?

    //    val offset: PointLike?
    val essential: Boolean?
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/camera.ts#L78 */
@JsPlainObject
external interface CenterZoomBearing {
    val center: LngLat?
    val zoom: Double?
    val bearing: Double?
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/camera.ts#L57 */
@JsPlainObject
external interface CameraOptions : CenterZoomBearing {
    val pitch: Double?
    val roll: Double?
    val elevation: Double?
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/camera.ts#L188 */
@JsPlainObject
external interface EaseToOptions : AnimationOptions, CameraOptions {
//    delayEndEvents?: number;
//    padding?: number | PaddingOptions;
//    around?: LngLatLike;
//    easeId?: string;
//    noMoveStart?: boolean;
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/camera.ts#L142 */
@JsPlainObject
external interface FlyToOptions : AnimationOptions, CameraOptions {
//    curve?: number;
//    minZoom?: number;
//    speed?: number;
//    screenSpeed?: number;
//    maxDuration?: number;
//    padding?: number | PaddingOptions;
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/geo/lng_lat.ts#L51 */
external class LngLat(val lng: Double, val lat: Double)

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/util/evented.ts#L6 */
typealias Listener<T> = (event: T) -> Any

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/util/util.ts#L974 */
external interface Subscription {
    fun unsubscribe()
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/index.ts#L176 */
external fun setWorkerUrl(value: String)