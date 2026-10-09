package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.dom.DOMElement
import com.lightningkite.kiteui.map.maplibre.EaseToOptions
import com.lightningkite.kiteui.map.maplibre.FlyToOptions
import com.lightningkite.kiteui.map.maplibre.Marker
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.map.maplibre.Map as MapLibreMap
import com.lightningkite.kiteui.map.maplibre.setWorkerUrl
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.reactive.extensions.value
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlin.reflect.KMutableProperty1

actual class MapView actual constructor(
    context: ElementContext, val interactive: Boolean
) : NativeElement(context), Map.BaseProperties {
    internal actual val preInit = PreInit()
    val map = LateInitSignal<MapLibreMap>()

    actual override var style by lateInit(PreInit::style, {
        getStyleUrl()?.let { Map.Style.Url(it) } //?: Map.Style.Json(getStyle().toString())
    }) { setStyle(it?.toUnion()) }

    actual val camera = object : Map.Camera {
        override var center by lateInit(PreInit::center, { getCenter().toGeoCoordinate() }) { setCenter(it.toLngLat()) }
        override var zoom by lateInit(PreInit::zoom, MapLibreMap::getZoom, MapLibreMap::setZoom)
        override var minZoom by lateInit(PreInit::minZoom, MapLibreMap::getMinZoom, MapLibreMap::setMinZoom)
        override var maxZoom by lateInit(PreInit::maxZoom, MapLibreMap::getMaxZoom, MapLibreMap::setMaxZoom)
        override var pitch by lateInit(PreInit::pitch, MapLibreMap::getPitch, MapLibreMap::setPitch)
        override var minPitch by lateInit(PreInit::minPitch, MapLibreMap::getMinPitch, MapLibreMap::setMinPitch)
        override var maxPitch by lateInit(PreInit::maxPitch, MapLibreMap::getMaxPitch, MapLibreMap::setMaxPitch)

        override fun easeTo(options: Map.EaseToOptions) = lateRun {
            easeTo(
                EaseToOptions(
                    center = options.center?.toLngLat(),
                    zoom = options.zoom,
                    bearing = options.bearing,
                    pitch = options.pitch,
                    duration = options.duration.inWholeMilliseconds.toDouble(),
                )
            )
        }

        override fun flyTo(options: Map.FlyToOptions) = lateRun {
            flyTo(
                FlyToOptions(
                    center = options.center?.toLngLat(),
                    zoom = options.zoom,
                    bearing = options.bearing,
                    pitch = options.pitch,
                    duration = options.duration.inWholeMilliseconds.toDouble(),
                )
            )
        }

        override fun stopAnimation() {
            launch { map.awaitOnce().stop() }
        }
    }

    actual fun createMarker(position: GeoCoordinate) = object : Map.Marker {
        val raw = Marker()

        init {
            raw.setLngLat(position.toLngLat())
            launch { raw.addTo(map.awaitOnce()) }
        }

        override fun update(data: GeoCoordinate) {
            raw.setLngLat(data.toLngLat())
        }

        override fun remove() {
            raw.remove()
        }
    }


    init {
        setWorkerUrl(maplibreWorkerUrl)

        native.tag = "div"
        native.id = "maplibre-map"

        injectMapLibreCss()

        native.onElement { map.value = MapLibreMap(preInit.toOptions(it, interactive)) }
    }
}


internal fun PreInit.toOptions(container: DOMElement, interactive: Boolean): MapLibreMap.Options {
    return MapLibreMap.Options(
        container = container,
        style = style?.toUnion(),
        interactive = interactive,

        center = center.toLngLat(),
        zoom = zoom,
        minZoom = minZoom ?: JsUndefined,
        maxZoom = maxZoom ?: JsUndefined,
        pitch = pitch,
        minPitch = minPitch ?: JsUndefined,
        maxPitch = maxPitch ?: JsUndefined,
    )
}

actual fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit) = lateRun {
    on("click") { callback(geoCoordinateFromLngLat(it.asDynamic().lngLat)) }
}

private fun <T> MapView.lateInit(
    preInitProp: KMutableProperty1<PreInit, T>,
    getter: MapLibreMap.() -> T,
    setter: MapLibreMap.(T) -> Unit,
) = LateInit(this, { map.state }, preInitProp, getter, setter)

private fun MapView.lateRun(run: MapLibreMap.() -> Unit) {
    map.state.getOrNull()?.run() ?: launch { map.awaitOnce().run() }
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