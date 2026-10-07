/**
 * External MapLibre TypeScript typedefs
 *
 * If the npm package version changes, this may become out of sync! Double check that the
 * current version of each linked source file is the same as the npm package version in
 * /library-map/build.gradle.kts before using this as the source of truth
 *
 * Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0
 */
@file:JsModule("maplibre-gl")
@file:JsNonModule

package com.lightningkite.kiteui.map.maplibre

import kotlinx.js.JsPlainObject
import org.w3c.dom.Element

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

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1480 */
    fun easeTo(options: EaseToOptions, eventData: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1517 */
    fun flyTo(options: FlyToOptions, eventData: Any? = definedExternally): Map

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1807 */
    fun getMinZoom(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1774 */
    fun setMinZoom(minZoom: Double? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1858 */
    fun getMaxZoom(): Double

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L1827 */
    fun setMaxZoom(maxZoom: Double? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2338 */
    fun on(type: String, listener: Listener<Any>): Subscription

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2680 */
    fun setStyle(style: Any?, options: Any? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2822 */
    fun getStyle(): Any

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2838 */
    fun getStyleUrl(): String?

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L84 */
    @JsPlainObject
    interface Options {
        val container: Element
        val style: Any?
        val center: LngLat?
        val zoom: Double?
        val minZoom: Double?
        val maxZoom: Double?
    }
}

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