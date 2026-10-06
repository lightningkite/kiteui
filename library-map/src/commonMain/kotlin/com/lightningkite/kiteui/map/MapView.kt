package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.write
import com.lightningkite.services.data.GeoCoordinate
import kotlin.time.Duration

object Map {
    sealed interface Style {
        data class Url(val url: String) : Style
        data class Json(val json: kotlinx.serialization.json.Json): Style
    }


    interface CameraOptions {
        val center: GeoCoordinate?
        val zoom: Double?
        val bearing: Double?
    }

    interface AnimationOptions {
        val duration: Duration?
        val easing: Easing

        sealed interface Easing {
            object Default : Easing
            object Linear : Easing
            data class CubicBezier(val x1: Double, val y1: Double, val x2: Double, val y2: Double) : Easing
        }
    }

    data class EaseToOptions(
        override val center: GeoCoordinate? = null,
        override val zoom: Double? = null,
        override val bearing: Double? = null,
        override val duration: Duration,
        override val easing: AnimationOptions.Easing.Default,
    ) : CameraOptions, AnimationOptions

    data class FlyToOptions(
        override val center: GeoCoordinate? = null,
        override val zoom: Double? = null,
        override val bearing: Double? = null,
        override val duration: Duration,
        override val easing: AnimationOptions.Easing.Default,
    ) : CameraOptions, AnimationOptions
}

expect class MapView(context: ElementContext) : NativeElement {
    var style: Map.Style?
    var center: GeoCoordinate

    value class Camera(private val view: MapView) {
        suspend fun easeTo(options: Map.EaseToOptions)
        suspend fun flyTo(options: Map.FlyToOptions)
    }
    val camera: Camera
}

inline fun ElementWriter.mapView(setup: MapView.() -> Unit) =
    write(MapView(context), setup)