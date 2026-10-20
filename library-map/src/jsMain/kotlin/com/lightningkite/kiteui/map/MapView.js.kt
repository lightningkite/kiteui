package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.dom.DOMElement
import com.lightningkite.kiteui.map.maplibre.EaseToOptions
import com.lightningkite.kiteui.map.maplibre.FlyToOptions
import com.lightningkite.kiteui.map.maplibre.AddLayerObject
import com.lightningkite.kiteui.map.maplibre.GeoJSONSource
import com.lightningkite.kiteui.map.maplibre.SourceSpecification
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageResource
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ImageVector
import com.lightningkite.kiteui.models.vectorToSvgDataUrl
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.direct.createObjectURL
import com.lightningkite.kiteui.views.direct.revokeObjectURL
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.map.maplibre.Map as MapLibreMap
import com.lightningkite.kiteui.map.maplibre.setWorkerUrl
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.reactive.extensions.value
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.toJson
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.Image
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty1
import kotlin.uuid.Uuid

actual class MapView actual constructor(
    context: ElementContext, val disableAttribution: I_SWEAR_ON_MY_JOB?, val interactive: Boolean
) : NativeElement(context), BaseProperties {
    internal actual val preInit = PreInit()
    val map = LateInitSignal<MapLibreMap>()
    var isStyleLoaded = false

    actual override var style by lateInit(PreInit::style, {
        getStyleUrl()?.let { Style.Url(it) } ?: Style.Json(getStyle().toString())
    }) { setStyle(it.toUnion(), js("{diff: false}")); isStyleLoaded = false }

    actual val sources: MutableMap<Uuid, GeoJsonObject> = StyleDataMap({ id, data ->
        val map = map.state.getOrNull() ?: return@StyleDataMap
        if (!isStyleLoaded) return@StyleDataMap
        val existing = map.getSource(id.toString())?.unsafeCast<GeoJSONSource>()

        if (existing != null) return@StyleDataMap existing.setData(JSON.parse(data.toJson()))
        map.addSource(id.toString(), data.toSourceSpecification())
    }) { if (isStyleLoaded) map.state.getOrNull()?.removeSource(it.toString()) }

    actual val layers: MutableMap<Uuid, Layer<*>?> = StyleDataMap({ id, layer ->
        val map = map.state.getOrNull() ?: return@StyleDataMap
        if (!isStyleLoaded) return@StyleDataMap
        val beforeIndex = layers.keys.indexOf(id).let { if (0 <= it && it < layers.size - 1) it + 1 else null }
        val beforeId = beforeIndex?.let { layers.keys.toList()[it] }?.toString()

        if (map.getLayer(id.toString()) != null) map.removeLayer(id.toString())
        map.addLayer(layer.toAddLayerObject(id), beforeId = beforeId)
    }) { if (isStyleLoaded) map.state.getOrNull()?.removeLayer(it.toString()) }

    /** The decoded [images], which can be re-added without reloading when the style changes. */
    private val loadedImages = HashMap<Uuid, LoadedImage>()

    private class LoadedImage(val element: HTMLImageElement, val pixelRatio: Double) {
        fun addTo(map: MapLibreMap, id: Uuid) {
            val pixelRatio = pixelRatio
            if (map.hasImage(id.toString())) map.removeImage(id.toString())
            map.addImage(id.toString(), element, js("{pixelRatio: pixelRatio}"))
        }
    }

    actual val images: MutableMap<Uuid, ImageSource> = StyleDataMap({ id, source ->
        loadImage(source) { image ->
            if (images[id] !== source) return@loadImage
            loadedImages[id] = image
            if (isStyleLoaded) map.state.getOrNull()?.let { image.addTo(it, id) }
        }
    }) {
        loadedImages.remove(it)
        if (isStyleLoaded) map.state.getOrNull()?.removeImage(it.toString())
    }

    private fun loadImage(source: ImageSource, onLoaded: (LoadedImage) -> Unit) {
        val element = Image()
        // MapLibre reads the pixels through a canvas, which cross-origin images would taint.
        element.crossOrigin = "anonymous"
        val blobUrl = when (source) {
            is ImageRaw -> createObjectURL(source.data)
            is ImageLocal -> createObjectURL(source.file)
            else -> null
        }
        element.onload = {
            blobUrl?.let(::revokeObjectURL)
            // Vectors have no natural resolution, so rasterize them at the screen's.
            val pixelRatio = if (source is ImageVector) window.devicePixelRatio else 1.0
            element.width = (element.naturalWidth * pixelRatio).roundToInt()
            element.height = (element.naturalHeight * pixelRatio).roundToInt()
            onLoaded(LoadedImage(element, pixelRatio))
        }
        element.onerror = { _, _, _, _, _ -> blobUrl?.let(::revokeObjectURL) }
        element.src = when (source) {
            is ImageRemote -> source.url
            is ImageResource -> context.basePath + source.relativeUrl
            is ImageVector -> source.vectorToSvgDataUrl()
            is ImageRaw, is ImageLocal -> blobUrl!!
        }
    }

    private fun GeoJsonObject.toSourceSpecification() = js("{}").unsafeCast<SourceSpecification>().also {
        it.asDynamic().type = "geojson"
        it.asDynamic().data = JSON.parse(toJson())
    }

    val emptySourceId = Uuid.random()
    private fun Layer<*>?.toAddLayerObject(id: Uuid): AddLayerObject = if (this == null) {
        val id = id.toString()
        val sourceId = emptySourceId.toString()
        js("{id: id, type: 'symbol', source: sourceId}")
    } else JSON.parse<AddLayerObject>(toJson().toString()).also { it.asDynamic().id = id.toString() }

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
                on("style.load") {
                    isStyleLoaded = true
                    loadedImages.forEach { (id, image) -> image.addTo(this, id) }
                    addSource(emptySourceId.toString(), js("{type: 'geojson', data: {type: 'FeatureCollection', features: []}}"))
                    sources.forEach { (id, source) -> addSource(id.toString(), source.toSourceSpecification()) }
                    layers.forEach { (id, layer) -> addLayer(layer.toAddLayerObject(id)) }
                }
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

        // Symbols fade in when placed, and a moved symbol counts as new, so it would blink on every move.
        fadeDuration = 0.0,
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