package com.lightningkite.kiteui.map

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.models.ImageRaw
import com.lightningkite.kiteui.models.ImageRemote
import com.lightningkite.kiteui.models.ImageResource
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.ImageVector
import com.lightningkite.kiteui.views.Path.toDrawable
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
import org.maplibre.android.style.layers.TransitionOptions
import org.maplibre.android.style.sources.GeoJsonSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.spatialk.geojson.toJson
import kotlin.reflect.KMutableProperty1
import kotlin.run
import kotlin.uuid.Uuid
import org.maplibre.android.maps.Style as MapLibreStyle
import org.maplibre.android.maps.MapView as MapLibreMapView
import androidx.core.graphics.drawable.toBitmap

actual class MapView actual constructor(
    context: ElementContext, val disableAttribution: I_SWEAR_ON_MY_JOB?, val interactive: Boolean
) : NativeElement(context), BaseProperties {
    internal actual val preInit = PreInit()
    val map = LateInitSignal<MapLibreMap>()

    actual override var style by lateInit(
        PreInit::style, { style.toMapStyle() }) { setStyle(it.toBuilder(), ::onStyleLoaded) }

    actual val sources: MutableMap<Uuid, Source<*, *>> = StyleDataMap({ id, source ->
        val style = map.state.getOrNull()?.style ?: return@StyleDataMap
        val existing = style.getSourceAs<GeoJsonSource>(id.toString())

        if (existing != null) return@StyleDataMap existing.setGeoJson(source.data.toJson())
        style.addSource(GeoJsonSource(id.toString(), source.data.toJson()))
    }) { map.state.getOrNull()?.style?.removeSource(it.toString()) }

    actual val layers: MutableMap<Uuid, Layer<*>?> = StyleDataMap({ id, layer ->
        val style = map.state.getOrNull()?.style ?: return@StyleDataMap
        val beforeIndex = layers.keys.indexOf(id).let { if (0 <= it && it < layers.size - 1) it + 1 else null }
        val beforeId = beforeIndex?.let { layers.keys.toList()[it] }?.toString()

        style.removeLayer(id.toString())
        val nativeLayer = layer.toMapLibreLayer(id.toString())
        if (beforeId == null) return@StyleDataMap style.addLayer(nativeLayer)
        style.addLayerBelow(nativeLayer, beforeId)
    }) { map.state.getOrNull()?.style?.removeLayer(it.toString()) }

    /** The decoded [images], which can be re-added without reloading when the style changes. */
    private val loadedImages = HashMap<Uuid, Bitmap>()

    actual val images: MutableMap<Uuid, ImageSource> = StyleDataMap({ id, source ->
        loadBitmap(source) { bitmap ->
            if (images[id] !== source) return@loadBitmap
            loadedImages[id] = bitmap
            map.state.getOrNull()?.style?.addImage(id.toString(), bitmap)
        }
    }) {
        loadedImages.remove(it)
        map.state.getOrNull()?.style?.removeImage(it.toString())
    }

    private fun loadBitmap(source: ImageSource, onLoaded: (Bitmap) -> Unit) {
        fun RequestBuilder<Bitmap>.finish() = disallowHardwareConfig().into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) =
                onLoaded(resource.copy(Bitmap.Config.ARGB_8888, false))

            override fun onLoadCleared(placeholder: Drawable?) {}
        })

        val glide = Glide.with(context.activity.applicationContext).asBitmap()
        when (source) {
            is ImageLocal -> glide.load(source.file.uri).finish()
            is ImageRaw -> BitmapFactory.decodeByteArray(source.data.data, 0, source.data.data.size)?.let(onLoaded)
            is ImageRemote -> glide.load(source.url).finish()
            is ImageResource -> glide.load(source.resource).finish()
            is ImageVector -> onLoaded(source.toDrawable().toBitmap())
        }
    }

    val emptySourceId = Uuid.random()
    private fun onStyleLoaded(style: MapLibreStyle) {
        // Symbols fade in when placed, and a moved symbol counts as new, so it would blink on every move.
        style.transition.let { style.transition = TransitionOptions(it.duration, it.delay, false) }
        style.addSource(GeoJsonSource(emptySourceId.toString()))
        loadedImages.forEach { (id, bitmap) -> style.addImage(id.toString(), bitmap) }
        sources.forEach { (id, source) -> style.addSource(GeoJsonSource(id.toString(), source.data.toJson())) }
        layers.forEach { (id, layer) -> style.addLayer(layer.toMapLibreLayer(id.toString())) }
    }

    private fun Layer<*>?.toMapLibreLayer(id: String): MapLibreLayer = if (this == null) {
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
        override var bearing by lateInit(
            PreInit::bearing, { cameraPosition.bearing }) { moveCamera(CameraUpdateFactory.bearingTo(it)) }
        override var padding by lateInit(
            PreInit::padding, {
                cameraPosition.padding?.toPadding() ?: Camera.Padding()
            }) { moveCamera(CameraUpdateFactory.paddingTo(it.toDoubleArray())) }
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

        getMapAsync { map ->
            preInit.toOptions(map, ::onStyleLoaded)
            map.uiSettings.isAttributionEnabled = disableAttribution == null
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.setAllGesturesEnabled(interactive)

            map.addOnMapClickListener { point ->
                val screenPoint = map.projection.toScreenLocation(point)
                handleSourceLayerClicks { layerIds ->
                    map.queryRenderedFeatures(screenPoint, *layerIds.map { it.toString() }.toTypedArray())
                        .map { feature ->
                            feature.properties()?.let { Json.parseToJsonElement(it.toString()).jsonObject }
                        }
                }
            }

            this@MapView.map.value = map
        }
    }
}

fun Camera.Padding.toDoubleArray(): DoubleArray = doubleArrayOf(left, top, right, bottom)
fun DoubleArray.toPadding() = Camera.Padding(
    left = this[0],
    top = this[1],
    right = this[2],
    bottom = this[3],
)

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
        padding?.let { padding(it.toDoubleArray()) }
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

    json["filter"]?.let { layer.setFilter(it.toNativeFilter()) }

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

private fun <T> MapView.lateInit(
    preInitProp: KMutableProperty1<PreInit, T>,
    getter: MapLibreMap.() -> T,
    setter: MapLibreMap.(T) -> Unit,
) = LateInit(this, { map.state }, preInitProp, getter, setter)

private fun MapView.lateRun(run: MapLibreMap.() -> Unit) {
    map.state.getOrNull()?.run() ?: launch { map.awaitOnce().run() }
}