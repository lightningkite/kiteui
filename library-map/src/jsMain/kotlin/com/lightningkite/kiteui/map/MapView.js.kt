
package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal

/** Vite bundles MapLibre's worker script in a place MapLibre doesn't expect, so we have to override this */
@JsModule("maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url")
@JsNonModule
private external val maplibreWorkerUrl: String

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    actual val api: LateInitSignal<MapApi> get() = TODO("Not yet implemented")
    lateinit var map: Map

    init {
        setWorkerUrl(maplibreWorkerUrl)

        native.tag = "div"
        native.id = "maplibre-map"

        native.onElement { container ->
            map = Map(Map.Options(
                container = container,
                style = "https://demotiles.maplibre.org/style.json",
            ))
        }
    }
}