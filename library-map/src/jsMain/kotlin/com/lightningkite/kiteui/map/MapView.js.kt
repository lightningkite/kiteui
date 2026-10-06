package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.map.maplibre.EaseToOptions
import com.lightningkite.kiteui.map.maplibre.FlyToOptions
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.map.maplibre.Map as MapLibreMap
import com.lightningkite.kiteui.map.maplibre.setWorkerUrl
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.reactive.extensions.value
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.browser.document

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    val map = LateInitSignal<MapLibreMap>()

    actual var style: Map.Style? = Map.Style.Demo
        get() {
            val map = map.state.getOrNull() ?: return field
            val styleUrl = map.getStyleUrl()?.let { Map.Style.Url(it) }
            return styleUrl ?: Map.Style.Json(map.getStyle())
        }
        set(value) {
            field = value
            map.state.getOrNull()?.setStyle(value?.toUnion())
        }

    actual var center = GeoCoordinate(0.0, 0.0)
        get() = map.state.getOrNull()?.getCenter()?.toGeoCoordinate() ?: field
        set(value) {
            field = value
            map.state.getOrNull()?.setCenter(value.toLngLat())
        }

    actual value class Camera(val view: MapView) {
        actual suspend fun easeTo(options: Map.EaseToOptions) {
            view.map.awaitOnce().easeTo(EaseToOptions(
                center = options.center?.toLngLat(),
                zoom = options.zoom,
                bearing = options.bearing,
                duration = options.duration.inWholeMilliseconds.toDouble(),
            ))
        }

        actual suspend fun flyTo(options: Map.FlyToOptions) {
            view.map.awaitOnce().flyTo(FlyToOptions(
                center = options.center?.toLngLat(),
                zoom = options.zoom,
                bearing = options.bearing,
                duration = options.duration.inWholeMilliseconds.toDouble(),
            ))
        }
    }

    actual val camera = Camera(this)

    init {
        setWorkerUrl(maplibreWorkerUrl)

        native.tag = "div"
        native.id = "maplibre-map"

        injectMapLibreCss()

        native.onElement { container ->
            map.value = MapLibreMap(
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

@JsModule("maplibre-gl/dist/maplibre-gl.css?inline")
@JsNonModule
private external val maplibreCss: String

private var maplibreCssInjected = false
private fun injectMapLibreCss() {
    if (maplibreCssInjected) return
    maplibreCssInjected = true
    document.head!!.appendChild(document.createElement("style").apply { textContent = maplibreCss })
}