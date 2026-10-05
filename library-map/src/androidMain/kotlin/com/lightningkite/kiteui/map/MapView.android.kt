package com.lightningkite.kiteui.map

import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.services.data.GeoCoordinate
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.rememberMapState
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    actual val api: LateInitSignal<Map.Api> get() = TODO("Not yet implemented")

    actual var style by mutableStateOf<Map.Style?>(null)
    actual var center by mutableStateOf(GeoCoordinate(0.0, 0.0))

    override val native: View
        get() = ComposeView(context.activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                val state = rememberMapState(
                    baseStyle = style?.toBaseStyle() ?: BaseStyle.Empty,
                    initialCameraPosition = CameraPosition(target = center.toPosition())
                )

                MaplibreMap(state = state)
            }
        }
}

fun Map.Style.toBaseStyle() = when (this) {
    is Map.Style.Json -> BaseStyle.Json("")
    is Map.Style.Url -> BaseStyle.Uri(url)
}

fun GeoCoordinate.toPosition() = Position(longitude, latitude)