package com.lightningkite.kiteui.map

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.services.data.GeoCoordinate
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraAnimation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraUpdate
import org.maplibre.compose.camera.CubicBezier
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.DefaultMapRuntime
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.overlay.MapOverlay
import org.maplibre.compose.overlay.include
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
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

    private val markers = mutableStateListOf<AndroidMarker>()

    private inner class AndroidMarker(position: GeoCoordinate) : Map.Marker {
        var position by mutableStateOf(position)

        override fun update(data: GeoCoordinate) {
            position = data
        }

        override fun remove() {
            markers.remove(this)
        }
    }

    val mapState = DefaultMapRuntime.instance.createMapState(
        baseStyle = style?.toBaseStyle() ?: BaseStyle.Empty,
        cameraPosition = CameraPosition(target = GeoCoordinate(0.0, 0.0).toPosition())
    ) {
        val markerSource = rememberGeoJsonSource(
            GeoJsonData.Features(FeatureCollection(markers.map { Feature(Point(it.position.toPosition()), null) }))
        )
        SymbolLayer(
            id = "kiteui-markers",
            source = markerSource,
            iconImage = image(DefaultMarkerPin, DpSize(27.dp, 41.dp)),
            iconAnchor = const(SymbolAnchor.Bottom),
            iconAllowOverlap = const(true),
            iconIgnorePlacement = const(true),
        )
    }

    @JvmInline
    actual value class Camera(val view: MapView) {
        actual suspend fun easeTo(options: Map.EaseToOptions) {
            view.launch {
                view.mapState.animateCamera(
                    options.toCameraUpdate(),
                    CameraAnimation.Ease(
                        duration = options.duration,
                        easing = options.easing.toCubicBezier(),
                    ),
                )
            }
        }

        actual suspend fun flyTo(options: Map.FlyToOptions) {
            view.launch {
                view.mapState.animateCamera(
                    options.toCameraUpdate(),
                    CameraAnimation.Fly(
                        duration = options.duration,
                        easing = options.easing.toCubicBezier(),
                    ),
                )
            }
        }
    }

    actual val camera = Camera(this)

    actual fun createMarker(position: GeoCoordinate): Map.Marker = AndroidMarker(position).also { markers += it }

    init {
        onRemove { mapState.close() }
    }

    override val native: View = ComposeView(context.activity).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        setContent {
            MaplibreMap(state = mapState) { include(MapOverlay.AttributionOnly) }
        }
    }
}

private object DefaultMarkerPin : Painter() {
    override val intrinsicSize = Size.Unspecified

    override fun DrawScope.onDraw() {
        val w = size.width
        val h = size.height
        val r = w / 2
        val pin = Path().apply {
            moveTo(r, h)
            cubicTo(r * 0.6f, h * 0.75f, 0f, h * 0.5f, 0f, r)
            arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
            cubicTo(w, h * 0.5f, r * 1.4f, h * 0.75f, r, h)
            close()
        }
        drawPath(pin, Color(0xFF3FB1CE))
        drawCircle(Color.White, radius = w * 0.2f, center = Offset(r, r))
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