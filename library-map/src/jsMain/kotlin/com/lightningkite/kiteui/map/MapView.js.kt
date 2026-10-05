package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.kiteui.map.maplibre.Map as MapLibreMap
import com.lightningkite.kiteui.map.maplibre.setWorkerUrl
import com.lightningkite.services.data.GeoCoordinate

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    actual val api: LateInitSignal<Map.Api> get() = TODO("Not yet implemented")
    lateinit var map: MapLibreMap

    actual var style: Map.Style? = null
        get() {
            if (!::map.isInitialized) return field
            val styleUrl = map.getStyleUrl()?.let { Map.Style.Url(it) }
            return styleUrl ?: Map.Style.Json(map.getStyle())
        }
        set(value) {
            field = value
            if (::map.isInitialized) map.setStyle(value)
        }

   actual var center = GeoCoordinate(0.0, 0.0)
       get() {
           if (!::map.isInitialized) return field
           return map.getCenter().toGeoCoordinate()
       }
       set(value) {
           field = value
           if (::map.isInitialized) map.setCenter(value.toLngLat())
       }

    init {
        setWorkerUrl(maplibreWorkerUrl)

        native.tag = "div"
        native.id = "maplibre-map"

        native.onElement { container ->
            map = MapLibreMap(
                MapLibreMap.Options(
                    container = container,
                    style = style?.toUnion(),
                    center = center.toLngLat(),
                )
            )
        }
    }
}

/** Vite bundles MapLibre's worker script in a place MapLibre doesn't expect, so we have to override this */
@JsModule("maplibre-gl/dist/maplibre-gl-worker.mjs?worker&url")
@JsNonModule
private external val maplibreWorkerUrl: String