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
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.reflect.KMutableProperty1
import kotlin.run
import org.maplibre.android.maps.Style as MapLibreStyle
import org.maplibre.android.maps.MapView as MapLibreMapView

actual class MapView actual constructor(
    context: ElementContext, val disableAttribution: I_SWEAR_ON_MY_JOB?, val interactive: Boolean
) : NativeElement(context), BaseProperties {
    internal actual val preInit = PreInit()
    val map = LateInitSignal<MapLibreMap>()

    actual override var style by lateInit(PreInit::style, { style.toMapStyle() }) { setStyle(it.toBuilder()) }

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

    actual fun createMarker(position: GeoCoordinate) = object : Marker {
        var current = position
        var raw: org.maplibre.android.annotations.Marker? = null
        var removed = false

        init {
            launch {
                val map = map.awaitOnce()
                if (!removed) raw = map.addMarker(MarkerOptions().position(current.toLatLng()))
            }
        }

        override fun update(data: GeoCoordinate) {
            current = data
            raw?.setPosition(data.toLatLng())
        }

        override fun remove() {
            removed = true
//            raw?.let { r -> ifReady { removeMarker(r) } }
            raw = null
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
            preInit.toOptions(it)
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


internal fun PreInit.toOptions(map: MapLibreMap) {
    map.setStyle(style.toBuilder())
    map.cameraPosition = CameraPosition.Builder().target(center.toLatLng()).zoom(zoom).tilt(pitch).build()
    minZoom?.let { map.setMinZoomPreference(it) }
    maxZoom?.let { map.setMaxZoomPreference(it) }
    minPitch?.let { map.setMinPitchPreference(it) }
    maxPitch?.let { map.setMaxPitchPreference(it) }
}

private fun <T> MapView.lateInit(
    preInitProp: KMutableProperty1<PreInit, T>,
    getter: MapLibreMap.() -> T,
    setter: MapLibreMap.(T) -> Unit,
) = LateInit(this, { map.state }, preInitProp, getter, setter)

private fun MapView.lateRun(run: MapLibreMap.() -> Unit) {
    map.state.getOrNull()?.run() ?: launch { map.awaitOnce().run() }
}
