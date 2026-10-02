package com.lightningkite.kiteui.map

import android.view.View
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    actual val api: LateInitSignal<MapApi> get() = TODO("Not yet implemented")
    override val native: View get() = TODO("Not yet implemented")
}