package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.dom.DOMElement
import com.lightningkite.kiteui.map.maplibre.EaseToOptions
import com.lightningkite.kiteui.map.maplibre.FlyToOptions
import com.lightningkite.kiteui.map.maplibre.Marker
import com.lightningkite.kiteui.map.maplibre.Subscription
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.reflect.KMutableProperty1

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    val map = LateInitSignal<MapLibreMap>()

    internal actual val preInit = PreInit()

    actual var style by lateInit(PreInit::style, {
        getStyleUrl()?.let { Map.Style.Url(it) } //?: Map.Style.Json(getStyle().toString())
    }) { setStyle(it?.toUnion()) }

    actual inner class Camera {
        actual var center by lateInit(PreInit::center, { getCenter().toGeoCoordinate() }) { setCenter(it.toLngLat()) }
        actual var zoom by lateInit(PreInit::zoom, MapLibreMap::getZoom, MapLibreMap::setZoom)
        actual var minZoom by lateInit(PreInit::minZoom, MapLibreMap::getMinZoom, MapLibreMap::setMinZoom)
        actual var maxZoom by lateInit(PreInit::maxZoom, MapLibreMap::getMaxZoom, MapLibreMap::setMaxZoom)
        actual var pitch by lateInit(PreInit::pitch, MapLibreMap::getPitch, MapLibreMap::setPitch)
        actual var minPitch by lateInit(PreInit::minPitch, MapLibreMap::getMinPitch, MapLibreMap::setMinPitch)
        actual var maxPitch by lateInit(PreInit::maxPitch, MapLibreMap::getMaxPitch, MapLibreMap::setMaxPitch)

        actual suspend fun easeTo(options: Map.EaseToOptions) {
            val map = map.awaitOnce()
            map.awaitAnimation { eventData ->
                map.easeTo(
                    EaseToOptions(
                        center = options.center?.toLngLat(),
                        zoom = options.zoom,
                        bearing = options.bearing,
                        pitch = options.pitch,
                        duration = options.duration.inWholeMilliseconds.toDouble(),
                    ),
                    eventData,
                )
            }
        }

        actual suspend fun flyTo(options: Map.FlyToOptions) {
            val map = map.awaitOnce()
            map.awaitAnimation { eventData ->
                map.flyTo(
                    FlyToOptions(
                        center = options.center?.toLngLat(),
                        zoom = options.zoom,
                        bearing = options.bearing,
                        pitch = options.pitch,
                        duration = options.duration.inWholeMilliseconds.toDouble(),
                    ),
                    eventData,
                )
            }
        }

        actual fun stopAnimation() {
            launch { map.awaitOnce().stop() }
        }
    }

    actual val camera = Camera()

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

        native.onElement { map.value = MapLibreMap(preInit.toOptions(it)) }
    }
}



private var nextAnimationId = 0

/**
 * MapLibre fires "moveend" when an animation finishes or is interrupted (by [MapLibreMap.stop], a new
 * animation, or user interaction), merging the animation's eventData into the event. We tag each
 * animation with a unique id so we only resume on the "moveend" belonging to this animation.
 */
private suspend fun MapLibreMap.awaitAnimation(start: (eventData: dynamic) -> Unit) =
    suspendCancellableCoroutine { cont ->
        val id = nextAnimationId++
        val eventData: dynamic = js("({})")
        eventData.kiteuiAnimationId = id
        var subscription: Subscription? = null
        subscription = on("moveend") { event ->
            if (event.asDynamic().kiteuiAnimationId == id) {
                subscription?.unsubscribe()
                if (cont.isActive) cont.resume(Unit)
            }
        }
        cont.invokeOnCancellation {
            subscription?.unsubscribe()
            stop()
        }
        start(eventData)
    }

internal fun PreInit.toOptions(container: DOMElement): MapLibreMap.Options {
    return MapLibreMap.Options(
        container = container,
        style = style?.toUnion(),
        center = center.toLngLat(),
        zoom = zoom,
        minZoom = minZoom ?: JsUndefined,
        maxZoom = maxZoom ?: JsUndefined,
        pitch = pitch,
        minPitch = minPitch ?: JsUndefined,
        maxPitch = maxPitch ?: JsUndefined,
    )
}

private fun <T> MapView.lateInit(
    preInitProp: KMutableProperty1<PreInit, T>,
    getter: MapLibreMap.() -> T,
    setter: MapLibreMap.(T) -> Unit,
) = LateInit(this, { map.state }, preInitProp, getter, setter)



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