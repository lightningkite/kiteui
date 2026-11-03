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
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.layers.Layer as MapLibreLayer
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
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
    internal val map = LateInitSignal<MapLibreMap>()

    actual override var style by lateInit(
        PreInit::style, { style.toMapStyle() }) { setStyle(it.toBuilder(), ::onStyleLoaded) }

    actual val sources: MutableMap<Uuid, Source<*, *>> = StyleDataMap({ id, source ->
        val style = map.state.getOrNull()?.style ?: return@StyleDataMap
        val existing = style.getSourceAs<GeoJsonSource>(id.toString())

        if (existing != null) return@StyleDataMap existing.setGeoJson(source.data)
        style.addSource(GeoJsonSource(id.toString(), source.data))
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

    private val emptySourceId = Uuid.random()
    private fun onStyleLoaded(style: MapLibreStyle) {
        style.addSource(GeoJsonSource(emptySourceId.toString()))
        loadedImages.forEach { (id, bitmap) -> style.addImage(id.toString(), bitmap) }
        sources.forEach { (id, source) -> style.addSource(GeoJsonSource(id.toString(), source.data)) }
        layers.forEach { (id, layer) -> style.addLayer(layer.toMapLibreLayer(id.toString())) }
    }

    private fun Layer<*>?.toMapLibreLayer(id: String): MapLibreLayer = if (this == null) {
        SymbolLayer(id, emptySourceId.toString())
    } else layerFromJson(id)

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

actual fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit) = lateRun {
    addOnMapClickListener { callback(it.toGeoCoordinate()); true }
}


private fun <T> MapView.lateInit(
    preInitProp: KMutableProperty1<PreInit, T>,
    getter: MapLibreMap.() -> T,
    setter: MapLibreMap.(T) -> Unit,
) = LateInit(this, { map.state }, preInitProp, getter, setter)

private fun MapView.lateRun(run: MapLibreMap.() -> Unit) {
    map.state.getOrNull()?.run() ?: launch { map.awaitOnce().run() }
}