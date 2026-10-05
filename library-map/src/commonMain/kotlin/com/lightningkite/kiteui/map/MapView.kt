package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.services.data.GeoCoordinate

object Map {
    interface Api

    sealed interface Style {
        data class Url(val url: String) : Style
        data class Json(val json: kotlinx.serialization.json.Json): Style
    }
}

expect class MapView(context: ElementContext) : NativeElement {
    val api: LateInitSignal<Map.Api>

    var style: Map.Style?
    var center: GeoCoordinate
}

inline fun ElementWriter.mapView(setup: MapView.() -> Unit) =
    write(MapView(context), setup)