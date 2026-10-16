package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.map.style.Layer
import com.lightningkite.services.data.GeoCoordinate
import org.maplibre.spatialk.geojson.GeoJsonObject
import kotlin.uuid.Uuid

actual class MapView actual constructor(
    context: ElementContext, disableAttribution: I_SWEAR_ON_MY_JOB?, val interactive: Boolean
) : NativeElement(context), BaseProperties {
    init {
        native.tag = "div"
        native.classes.add("map-view-ssr-placeholder")
    }

    actual val sources: MutableMap<Uuid, GeoJsonObject> = LinkedHashMap()
    actual val layers: MutableMap<Uuid, Layer<*>?> = LinkedHashMap()

    actual override var style: Style = Style.Demo
    internal actual val preInit: PreInit
        get() = TODO("Not yet implemented")
    actual val camera: Camera
        get() = TODO("Not yet implemented")
}

actual fun MapView.onClick(callback: (where: GeoCoordinate) -> Unit) {
}
