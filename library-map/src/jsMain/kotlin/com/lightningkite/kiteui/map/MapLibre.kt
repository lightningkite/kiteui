/**
 * External MapLibre TypeScript typedefs
 *
 * If the npm package version changes, this may become out of sync! Double check that
 * the current version of the linked source files are the same as the npm package
 * version in /library-map/build.gradle.kts before using this as the source of truth
 *
 * Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0
 */
@file:JsModule("maplibre-gl")
@file:JsNonModule

package com.lightningkite.kiteui.map

import kotlinx.js.JsPlainObject
import org.w3c.dom.Element

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts */
external class Map(options: Options) {
    /** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/ui/map.ts#L84 */
    @JsPlainObject
    interface Options {
        val container: Element?
        val style: String?
    }
}

/** Defined at https://github.com/maplibre/maplibre-gl-js/blob/v6.12.0/src/index.ts#L176 */
external fun setWorkerUrl(value: String)