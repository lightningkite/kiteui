package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.core.LateInitSignal

interface MapApi

expect class MapView(context: ElementContext) : NativeElement {
    val api: LateInitSignal<MapApi>
}

inline fun ElementWriter.mapView(setup: MapView.() -> Unit) =
    write(MapView(context), setup)