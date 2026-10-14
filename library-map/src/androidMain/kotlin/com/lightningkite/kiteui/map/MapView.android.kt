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
import org.maplibre.android.style.layers.Layer as MapLibreLayer
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
import com.lightningkite.kiteui.map.style.Layer
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

    actual override var style by lateInit(PreInit::style, { style.toMapStyle() }) {
        setStyle(it.toBuilder(), ::onStyleLoaded)
        preInit.style = it
    }

    actual val sources: MutableMap<Uuid, GeoJsonObject> = StyleDataMap({ id, data ->
        val style = map.state.getOrNull()?.style ?: return@StyleDataMap
        val existing = style.getSourceAs<GeoJsonSource>(id.toString())

        if (existing != null) return@StyleDataMap existing.setGeoJson(data.toJson())
        style.addSource(GeoJsonSource(id.toString(), data.toJson()))
    }) { map.state.getOrNull()?.style?.removeSource(it.toString()) }

    actual val layers: MutableMap<Uuid, Layer?> = StyleDataMap({ id, layer ->
        val style = map.state.getOrNull()?.style ?: return@StyleDataMap
        val beforeIndex = layers.keys.indexOf(id).let { if (0 <= it && it < layers.size - 1) it + 1 else null }
        val beforeId = beforeIndex?.let { layers.keys.toList()[it] }?.toString()

        style.removeLayer(id.toString())
        val nativeLayer = layer.toMapLibreLayer(id.toString())
        if (beforeId == null) return@StyleDataMap style.addLayer(nativeLayer)
        style.addLayerBelow(nativeLayer, beforeId)
    }) { map.state.getOrNull()?.style?.removeLayer(it.toString()) }

    val emptySourceId = Uuid.random()
    private fun onStyleLoaded(style: MapLibreStyle) {
        style.addSource(GeoJsonSource(emptySourceId.toString()))
        sources.forEach { (id, source) -> style.addSource(GeoJsonSource(id.toString(), source.toJson())) }
        layers.forEach { (id, layer) -> style.addLayer(layer.toMapLibreLayer(id.toString())) }
    }

    private fun Layer?.toMapLibreLayer(id: String): MapLibreLayer = if (this == null) {
        SymbolLayer(id, emptySourceId.toString())
    } else layerFromJson(id, toJson())

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

private fun layerFromJson(id: String, json: JsonObject): MapLibreLayer {
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
    json["paint"]?.jsonObject?.forEach { (name, value) ->
        properties += PaintPropertyValue(
            name,
            value.toNativeValue()
        )
    }
    json["layout"]?.jsonObject?.forEach { (name, value) ->
        properties += LayoutPropertyValue(
            name,
            value.toNativeValue()
        )
    }
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