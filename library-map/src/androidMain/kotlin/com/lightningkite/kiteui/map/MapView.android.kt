package com.lightningkite.kiteui.map

import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.services.data.GeoCoordinate
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.camera.CubicBezier
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    actual var style: Map.Style? = Map.Style.Demo
        set(value) {
            field = value
            mapState.style.asMutable!!.baseStyle = value?.toBaseStyle() ?: BaseStyle.Empty
        }

    actual var center: GeoCoordinate
        get() = mapState.cameraPosition.target.let { GeoCoordinate(it.latitude, it.longitude) }
        set(value) {
            mapState.setCameraPosition(mapState.cameraPosition.copy(target = value.toPosition()))
        }

    val mapState = DefaultMapRuntime.instance.createMapState(
        baseStyle = style?.toBaseStyle() ?: BaseStyle.Empty,
        cameraPosition = CameraPosition(target = GeoCoordinate(0.0, 0.0).toPosition())
    )

    @JvmInline
    actual value class Camera(val view: MapView) {
        actual suspend fun easeTo(options: Map.EaseToOptions) {
            view.mapState.animateCamera(
                options.toCameraUpdate(),
                CameraAnimation.Ease(
                    duration = options.duration,
                    easing = options.easing.toCubicBezier(),
                ),
            )
        }

        actual suspend fun flyTo(options: Map.FlyToOptions) {
            view.mapState.animateCamera(
                options.toCameraUpdate(),
                CameraAnimation.Fly(
                    duration = options.duration,
                    easing = options.easing.toCubicBezier(),
                ),
            )
        }
    }

    actual val camera = Camera(this)

    init {
        onRemove { mapState.close() }
    }

    override val native: View = ComposeView(context.activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent { MaplibreMap(state = mapState) { include(MapOverlay.AttributionOnly) } }
    }
}

fun Map.Style.toBaseStyle() = when (this) {
    is Map.Style.Json -> BaseStyle.Json("")
    is Map.Style.Url -> BaseStyle.Uri(url)
}

fun GeoCoordinate.toPosition() = Position(longitude, latitude)

fun Map.CameraOptions.toCameraUpdate() = CameraUpdate(
    target = center?.toPosition(),
    zoom = zoom,
    bearing = bearing,
    tilt = null,
    padding = null,
)

fun Map.AnimationOptions.Easing.toCubicBezier() = when (this) {
    Map.AnimationOptions.Easing.Default -> CubicBezier.Default
    Map.AnimationOptions.Easing.Linear -> CubicBezier.Linear
    is Map.AnimationOptions.Easing.CubicBezier -> CubicBezier(x1, y1, x2, y2)
}