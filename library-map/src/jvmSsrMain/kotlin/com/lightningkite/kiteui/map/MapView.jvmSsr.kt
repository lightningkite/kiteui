package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.services.data.GeoCoordinate

actual class MapView actual constructor(
    context: ElementContext, val interactive: Boolean
) : NativeElement(context), Map.BaseProperties {
    init {
        native.tag = "div"
        native.classes.add("map-view-ssr-placeholder")
    }

    actual override var style: Map.Style? = null
    internal actual val preInit: PreInit
        get() = TODO("Not yet implemented")
    actual val camera: Map.Camera
        get() = TODO("Not yet implemented")

    actual fun createMarker(position: GeoCoordinate): Map.Marker {
        TODO("Not yet implemented")
    }
}

actual fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit) {
}