package com.lightningkite.kiteui.map

import cocoapods.MapLibre.*
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.kiteui.map.style.LayerWithSource
import com.lightningkite.services.data.GeoCoordinate
import org.maplibre.spatialk.geojson.GeoJsonObject
import kotlin.uuid.Uuid
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.Foundation.NSURL
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    private val mapView = MLNMapView()
    override val native: UIView get() = mapView

    actual val api: LateInitSignal<Map.Api> get() = TODO("Not yet implemented")

    actual val sources: MutableMap<Uuid, GeoJsonObject> = LinkedHashMap()
    actual val layers: MutableMap<Uuid, LayerWithSource> = LinkedHashMap()

    actual var style: Map.Style? = null
//        set(value) {
//            field = value
//            when (value) {
//                is Map.Style.Url -> mapView.styleURL = NSURL.URLWithString(value.url)
//                is Map.Style.Json -> { /* JSON styles not directly supported via MLNMapView URL; skip */ }
//                null -> { }
//            }
//        }

    actual var center: GeoCoordinate = GeoCoordinate(0.0, 0.0)
//        set(value) {
//            field = value
//            mapView.centerCoordinate = CLLocationCoordinate2DMake(value.latitude, value.longitude)
//        }
}
