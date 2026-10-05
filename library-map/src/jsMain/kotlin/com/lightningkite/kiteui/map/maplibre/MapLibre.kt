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

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2338 */
    fun on(type: String, listener: Listener<Any>): Subscription

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2680 */
    fun setStyle(style: Any?, options: Any? = definedExternally)

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2822 */
    fun getStyle(): kotlinx.serialization.json.Json

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L2838 */
    fun getStyleUrl(): String?

    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L84 */
    @JsPlainObject
    interface Options {
        val container: Element?
        val style: Any?
        val center: LngLat?
    }
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