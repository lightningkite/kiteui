
package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal

/**
 * Vite bundles MapLibre's worker script in a place MapLibre doesn't expect, so we have to override this
 *
 * URL of MapLibre's worker script, emitted by Vite as a standalone worker chunk.
 *
 * MapLibre finds its worker as a sibling of `maplibre-gl.mjs` via `import.meta.url`. Vite's dependency
 * pre-bundling moves that file into `.vite/deps/` (where no worker exists), and production builds inline it
 * into the app chunk, so neither dev nor prod can find the worker on their own. The `?worker&url` import
 * makes Vite bundle the worker (along with its `maplibre-gl-shared.mjs` import) and hand back its URL.
 */
@JsModule("maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url")
@JsNonModule
internal external val maplibreWorkerUrl: String

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    actual val api: LateInitSignal<MapApi> get() = TODO("Not yet implemented")
    lateinit var map: Map

    init {
        native.tag = "div"
        native.id = "maplibre-map"

        setWorkerUrl(maplibreWorkerUrl)

        native.onElement { container ->
            map = Map(Map.Options(
                container = container,
                style = "https://demotiles.maplibre.org/style.json",
            ))
        }
    }
}