package com.lightningkite.kiteui.map

import android.view.View
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.context.awaitOnce
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.reactive.extensions.value
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.ColorReliefLayer
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.HillshadeLayer
import org.maplibre.android.style.layers.Layer
import org.maplibre.android.style.layers.LayoutPropertyValue
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PaintPropertyValue
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.layers.RasterLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.lightningkite.kiteui.map.style.Layer as StyleLayer
import org.maplibre.spatialk.geojson.GeoJsonObject
import org.maplibre.spatialk.geojson.toJson
import kotlin.reflect.KMutableProperty1
import kotlin.run
import kotlin.uuid.Uuid
import org.maplibre.android.maps.Style as MapLibreStyle
import org.maplibre.android.maps.MapView as MapLibreMapView

actual class MapView actual constructor(
    context: ElementContext, val disableAttribution: I_SWEAR_ON_MY_JOB?, val interactive: Boolean
) : NativeElement(context), BaseProperties {
    internal actual val preInit = PreInit()
    val map = LateInitSignal<MapLibreMap>()

    actual override var style by lateInit(PreInit::style, {
        style.toMapStyle()
    }) {
        loadedStyle = null
        setStyle(it.toBuilder(), ::onStyleLoaded)
    }

    private var loadedStyle: MapLibreStyle? = null

    actual val sources: MutableMap<Uuid, GeoJsonObject> = StyleDataMap(::onSourcePut, ::onSourceRemove)
    actual val layers: MutableMap<Uuid, StyleLayer> = StyleDataMap(::onLayerPut, ::onLayerRemove)

    private fun onSourcePut(id: Uuid, old: GeoJsonObject?, data: GeoJsonObject) {
        val style = loadedStyle ?: return
        if (!style.hasSource(id)) {
            style.addGeoJsonSource(id, data)
            style.addMissingLayers { it.source == id }
        } else if (old !== data) {
            attemptMapChange("update source $id") {
                style.getSourceAs<GeoJsonSource>(id.toString())!!.setGeoJson(data.toJson())
            }
        }
    }

    private fun onSourceRemove(id: Uuid, old: GeoJsonObject) {
        val style = loadedStyle ?: return
        // MapLibre won't remove a source while layers use it. They're re-added if the source comes back.
        for ((layerId, layer) in layers) {
            if (layer.source == id) style.removeLayerIfPresent(layerId)
        }
        if (style.hasSource(id)) attemptMapChange("remove source $id") { style.removeSource(id.toString()) }
    }

    private fun onLayerPut(id: Uuid, old: StyleLayer?, layer: StyleLayer) {
        val style = loadedStyle ?: return
        // MapLibre can't replace a layer in place, but removal is synchronous, so re-adding works.
        style.removeLayerIfPresent(id)
        // A new key is last in the map, so its layer goes on top. A replaced one keeps its position.
        style.addStyleLayer(id, layer, beforeId = if (old == null) null else style.nextLayerAbove(id))
    }

    private fun onLayerRemove(id: Uuid, old: StyleLayer) {
        loadedStyle?.removeLayerIfPresent(id)
    }

    private fun onStyleLoaded(style: MapLibreStyle) {
        loadedStyle = style
        for ((id, data) in sources) style.addGeoJsonSource(id, data)
        style.addMissingLayers { true }
    }

    private fun MapLibreStyle.hasSource(id: Uuid) = getSource(id.toString()) != null
    private fun MapLibreStyle.hasLayer(id: Uuid) = getLayer(id.toString()) != null

    private fun MapLibreStyle.addGeoJsonSource(id: Uuid, data: GeoJsonObject) = attemptMapChange("add source $id") {
        addSource(GeoJsonSource(id.toString(), data.toJson()))
    }

    private fun MapLibreStyle.addStyleLayer(id: Uuid, layer: StyleLayer, beforeId: Uuid?): Boolean {
        if (!hasSource(layer.source)) return false
        return attemptMapChange("add layer $id") {
            val nativeLayer = layerFromJson(id.toString(), layer.toJson())
            if (beforeId == null) addLayer(nativeLayer) else addLayerBelow(nativeLayer, beforeId.toString())
        } != null
    }

    private fun MapLibreStyle.removeLayerIfPresent(id: Uuid) {
        if (hasLayer(id)) attemptMapChange("remove layer $id") { removeLayer(id.toString()) }
    }

    private fun MapLibreStyle.nextLayerAbove(id: Uuid): Uuid? =
        this@MapView.layers.keys.asSequence().dropWhile { it != id }.drop(1).firstOrNull { hasLayer(it) }

    private fun MapLibreStyle.addMissingLayers(predicate: (StyleLayer) -> Boolean) {
        var above: Uuid? = null
        for ((id, layer) in this@MapView.layers.entries.reversed()) {
            if (hasLayer(id) || (predicate(layer) && addStyleLayer(id, layer, above))) above = id
        }
    }

    actual val camera = object : Camera {
        override var center by lateInit(
            PreInit::center, {
                cameraPosition.target?.toGeoCoordinate() ?: GeoCoordinate(0.0, 0.0)
            }) { moveCamera(CameraUpdateFactory.newLatLng(it.toLatLng())) }
        override var zoom by lateInit(PreInit::zoom, { zoom }) { moveCamera(CameraUpdateFactory.zoomTo(it)) }
        override var minZoom by lateInit(PreInit::minZoom, { minZoomLevel }) { setMinZoomPreference(it ?: 0.0) }
        override var maxZoom by lateInit(PreInit::maxZoom, { maxZoomLevel }) { setMaxZoomPreference(it ?: 22.0) }
        override var pitch by lateInit(
            PreInit::pitch, { cameraPosition.tilt }) { moveCamera(CameraUpdateFactory.tiltTo(it)) }
        override var minPitch by lateInit(PreInit::minPitch, { minPitch }) { setMinPitchPreference(it ?: 0.0) }
        override var maxPitch by lateInit(PreInit::maxPitch, { maxPitch }) { setMaxPitchPreference(it ?: 90.0) }

        override fun ease(animateTo: Camera.AnimateTo) = lateRun {
            easeCamera(animateTo.toCameraUpdate(this), animateTo.duration.inWholeMilliseconds.toInt())
        }

        override fun fly(animateTo: Camera.AnimateTo) = lateRun {
            animateCamera(animateTo.toCameraUpdate(this), animateTo.duration.inWholeMilliseconds.toInt())
        }

        override fun stopAnimation() {
            launch { map.awaitOnce().cancelTransitions() }
        }
    }

    override val native: View = run {
        MapLibre.getInstance(context.activity)
        MapLibreMapView(context.activity)
    }.apply {
        onCreate(null)

        var destroyed = false

        onRemove {
            destroyed = true
            onDestroy()
        }

        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                if (destroyed) return
                onStart()
                onResume()
            }

            override fun onViewDetachedFromWindow(v: View) {
                if (destroyed) return
                onPause()
                onStop()
            }
        })

        getMapAsync {
            preInit.toOptions(it, ::onStyleLoaded)
            it.uiSettings.isAttributionEnabled = disableAttribution == null
            it.uiSettings.isLogoEnabled = false
            it.uiSettings.isCompassEnabled = false
            it.uiSettings.setAllGesturesEnabled(interactive)
            map.value = it
        }
    }
}

actual fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit) = lateRun {
    addOnMapClickListener { callback(it.toGeoCoordinate()); true }
}

private fun MapLibreStyle?.toMapStyle(): Style {
    if (this == null) return Style.Json("{}")
    return Style.Url(uri)
}

private fun Style?.toBuilder(): MapLibreStyle.Builder = when (this) {
    is Style.Json -> MapLibreStyle.Builder().fromJson(json)
    is Style.Url -> MapLibreStyle.Builder().fromUri(url)
    null -> MapLibreStyle.Builder().fromJson("""{"version":8,"sources":{},"layers":[]}""")
}

fun GeoCoordinate.toLatLng() = LatLng(latitude, longitude)
fun LatLng.toGeoCoordinate() = GeoCoordinate(latitude, longitude)

private fun Camera.AnimateTo.toCameraUpdate(map: MapLibreMap): CameraUpdate = CameraUpdateFactory.newCameraPosition(
    CameraPosition.Builder(map.cameraPosition).apply {
        center?.let { target(it.toLatLng()) }
        zoom?.let { zoom(it) }
        bearing?.let { bearing(it) }
        pitch?.let { tilt(it) }
    }.build()
)


internal fun PreInit.toOptions(map: MapLibreMap, onStyleLoaded: (MapLibreStyle) -> Unit) {
    map.setStyle(style.toBuilder(), onStyleLoaded)
    map.cameraPosition = CameraPosition.Builder().target(center.toLatLng()).zoom(zoom).tilt(pitch).build()
    minZoom?.let { map.setMinZoomPreference(it) }
    maxZoom?.let { map.setMaxZoomPreference(it) }
    minPitch?.let { map.setMinPitchPreference(it) }
    maxPitch?.let { map.setMaxPitchPreference(it) }
}

private fun layerFromJson(id: String, json: JsonObject): Layer {
    val type = json["type"]?.jsonPrimitive?.content
    val source = json["source"]?.jsonPrimitive?.content ?: ""
    val layer = when (type) {
        "background" -> BackgroundLayer(id)
        "circle" -> CircleLayer(id, source)
        "color-relief" -> ColorReliefLayer(id, source)
        "fill" -> FillLayer(id, source)
        "fill-extrusion" -> FillExtrusionLayer(id, source)
        "heatmap" -> HeatmapLayer(id, source)
        "hillshade" -> HillshadeLayer(id, source)
        "line" -> LineLayer(id, source)
        "raster" -> RasterLayer(id, source)
        "symbol" -> SymbolLayer(id, source)
        else -> throw IllegalArgumentException("Unsupported layer type '$type'")
    }

    json["minzoom"]?.let { layer.minZoom = it.jsonPrimitive.float }
    json["maxzoom"]?.let { layer.maxZoom = it.jsonPrimitive.float }

    json["filter"]?.let {
        val filter = Expression.Converter.convert(it.toString())
        when (layer) {
            is CircleLayer -> layer.setFilter(filter)
            is FillLayer -> layer.setFilter(filter)
            is FillExtrusionLayer -> layer.setFilter(filter)
            is HeatmapLayer -> layer.setFilter(filter)
            is LineLayer -> layer.setFilter(filter)
            is SymbolLayer -> layer.setFilter(filter)
            else -> throw IllegalArgumentException("Layer type '$type' does not support filters")
        }
    }

    val properties = ArrayList<PropertyValue<*>>()
    json["paint"]?.jsonObject?.forEach { (name, value) -> properties += PaintPropertyValue(name, value.toNativeValue()) }
    json["layout"]?.jsonObject?.forEach { (name, value) -> properties += LayoutPropertyValue(name, value.toNativeValue()) }
    layer.setProperties(*properties.toTypedArray())

    return layer
}

private fun JsonElement.toNativeValue(): Any? = when (this) {
    JsonNull -> null
    is JsonArray -> Array(size) { this[it].toNativeValue() }
    is JsonObject -> HashMap(mapValues { it.value.toNativeValue() })
    is JsonPrimitive -> if (isString) content else booleanOrNull ?: double
}

private fun <T> MapView.lateInit(
    preInitProp: KMutableProperty1<PreInit, T>,
    getter: MapLibreMap.() -> T,
    setter: MapLibreMap.(T) -> Unit,
) = LateInit(this, { map.state }, preInitProp, getter, setter)

private fun MapView.lateRun(run: MapLibreMap.() -> Unit) {
    map.state.getOrNull()?.run() ?: launch { map.awaitOnce().run() }
}
