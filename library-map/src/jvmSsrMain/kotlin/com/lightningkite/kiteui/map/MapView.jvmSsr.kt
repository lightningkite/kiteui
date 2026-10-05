package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.services.data.GeoCoordinate

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    init {
        native.tag = "div"
        native.classes.add("map-view-ssr-placeholder")
    }

    actual val api: LateInitSignal<Map.Api> get() = TODO("Not yet implemented")
    actual var style: Map.Style? = null
    actual var center: GeoCoordinate = GeoCoordinate(0.0, 0.0)
}
