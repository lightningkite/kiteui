package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.dom.DOMElement
import com.lightningkite.kiteui.map.maplibre.EaseToOptions
import com.lightningkite.kiteui.map.maplibre.FlyToOptions
import com.lightningkite.kiteui.map.maplibre.AddLayerObject
import com.lightningkite.kiteui.map.maplibre.GeoJSONSource
import com.lightningkite.kiteui.map.maplibre.SourceSpecification
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
import com.lightningkite.kiteui.map.style.LayerWithSource
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.toJson
import kotlin.reflect.KMutableProperty1
import kotlin.uuid.Uuid

actual class MapView actual constructor(
    context: ElementContext, val disableAttribution: I_SWEAR_ON_MY_JOB?, val interactive: Boolean
) : NativeElement(context), BaseProperties {
    internal actual val preInit = PreInit()
    val map = LateInitSignal<MapLibreMap>()

    actual override var style by lateInit(PreInit::style, {
        getStyleUrl()?.let { Style.Url(it) } ?: Style.Json(getStyle().toString())
    }) {
        loadedMap = null
        setStyle(it.toUnion(), js("{diff: false}"))
    }

    private var loadedMap: MapLibreMap? = null

    actual val sources: MutableMap<Uuid, GeoJsonObject> = StyleDataMap(::onSourcePut, ::onSourceRemove)
    actual val layers: MutableMap<Uuid, LayerWithSource> = StyleDataMap(::onLayerPut, ::onLayerRemove)

    private fun onSourcePut(id: Uuid, old: GeoJsonObject?, data: GeoJsonObject) {
        val map = loadedMap ?: return
        if (!map.hasSource(id)) {
            map.addGeoJsonSource(id, data)
            map.addMissingLayers { it.source == id }
        } else if (old !== data) {
            attemptMapChange("update source $id") {
                map.getSource(id.toString()).unsafeCast<GeoJSONSource>().setData(JSON.parse(data.toJson()))
            }
        }
    }

    private fun onSourceRemove(id: Uuid, old: GeoJsonObject) {
        val map = loadedMap ?: return
        for ((layerId, layer) in layers) {
            if (layer.source == id) map.removeLayerIfPresent(layerId)
        }
        if (map.hasSource(id)) attemptMapChange("remove source $id") { map.removeSource(id.toString()) }
    }

    private fun onLayerPut(id: Uuid, old: LayerWithSource?, layer: LayerWithSource) {
        val map = loadedMap ?: return
        if (old == layer && map.hasLayer(id)) return
        map.removeLayerIfPresent(id)
        map.addStyleLayer(id, layer, beforeId = if (old == null) null else map.nextLayerAbove(id))
    }

    private fun onLayerRemove(id: Uuid, old: LayerWithSource) {
        loadedMap?.removeLayerIfPresent(id)
    }

    private fun onStyleLoaded(map: MapLibreMap) {
        loadedMap = map
        for ((id, data) in sources) map.addGeoJsonSource(id, data)
        map.addMissingLayers { true }
    }

    private fun MapLibreMap.hasSource(id: Uuid) = getSource(id.toString()) != null
    private fun MapLibreMap.hasLayer(id: Uuid) = getLayer(id.toString()) != null

    private fun MapLibreMap.addGeoJsonSource(id: Uuid, data: GeoJsonObject) = attemptMapChange("add source $id") {
        addSource(id.toString(), js("{}").unsafeCast<SourceSpecification>().also {
            it.asDynamic().type = "geojson"
            it.asDynamic().data = JSON.parse(data.toJson())
        })
    }

    private fun MapLibreMap.addStyleLayer(id: Uuid, layer: LayerWithSource, beforeId: Uuid?): Boolean {
        if (!hasSource(layer.source)) return false
        return attemptMapChange("add layer $id") {
            val spec = JSON.parse<AddLayerObject>(layer.toJson().toString())
            spec.asDynamic().id = id.toString()
            addLayer(spec, beforeId?.toString())
        } != null
    }

    private fun MapLibreMap.removeLayerIfPresent(id: Uuid) {
        if (hasLayer(id)) attemptMapChange("remove layer $id") { removeLayer(id.toString()) }
    }

    private fun MapLibreMap.nextLayerAbove(id: Uuid): Uuid? =
        this@MapView.layers.keys.asSequence().dropWhile { it != id }.drop(1).firstOrNull { hasLayer(it) }

    private fun MapLibreMap.addMissingLayers(predicate: (LayerWithSource) -> Boolean) {
        var above: Uuid? = null
        for ((id, layer) in this@MapView.layers.entries.reversed()) {
            if (hasLayer(id) || (predicate(layer) && addStyleLayer(id, layer, above))) above = id
        }
    }

    actual val camera = object : Camera {
        override var center by lateInit(PreInit::center, { getCenter().toGeoCoordinate() }) { setCenter(it.toLngLat()) }
        override var zoom by lateInit(PreInit::zoom, MapLibreMap::getZoom, MapLibreMap::setZoom)
        override var minZoom by lateInit(PreInit::minZoom, MapLibreMap::getMinZoom, MapLibreMap::setMinZoom)
        override var maxZoom by lateInit(PreInit::maxZoom, MapLibreMap::getMaxZoom, MapLibreMap::setMaxZoom)
        override var pitch by lateInit(PreInit::pitch, MapLibreMap::getPitch, MapLibreMap::setPitch)
        override var minPitch by lateInit(PreInit::minPitch, MapLibreMap::getMinPitch, MapLibreMap::setMinPitch)
        override var maxPitch by lateInit(PreInit::maxPitch, MapLibreMap::getMaxPitch, MapLibreMap::setMaxPitch)

        override fun ease(animateTo: Camera.AnimateTo) = lateRun {
            easeTo(
                EaseToOptions(
                    center = animateTo.center?.toLngLat(),
                    zoom = animateTo.zoom,
                    bearing = animateTo.bearing,
                    pitch = animateTo.pitch,
                    duration = animateTo.duration.inWholeMilliseconds.toDouble(),
                )
            )
        }

        override fun fly(animateTo: Camera.AnimateTo) = lateRun {
            flyTo(
                FlyToOptions(
                    center = animateTo.center?.toLngLat(),
                    zoom = animateTo.zoom,
                    bearing = animateTo.bearing,
                    pitch = animateTo.pitch,
                    duration = animateTo.duration.inWholeMilliseconds.toDouble(),
                )
            )
        }

        override fun stopAnimation() {
            launch { map.awaitOnce().stop() }
        }
    }

    init {
        setWorkerUrl(maplibreWorkerUrl)

        native.tag = "div"
        native.id = "maplibre-map"

        injectMapLibreCss()

        native.onElement {
            map.value = MapLibreMap(preInit.toOptions(it, disableAttribution, interactive)).apply {
                on("style.load") { onStyleLoaded(this) }
            }
        }
    }
}


internal fun PreInit.toOptions(
    container: DOMElement,
    disableAttribution: I_SWEAR_ON_MY_JOB?,
    interactive: Boolean,
): MapLibreMap.Options {
    return MapLibreMap.Options(
        container = container,
        interactive = interactive,
        style = style.toUnion(),

        center = center.toLngLat(),
        zoom = zoom,
        minZoom = minZoom ?: JsUndefined,
        maxZoom = maxZoom ?: JsUndefined,
        pitch = pitch,
        minPitch = minPitch ?: JsUndefined,
        maxPitch = maxPitch ?: JsUndefined,
    ).also { if (disableAttribution != null) it.attributionControl = false }
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