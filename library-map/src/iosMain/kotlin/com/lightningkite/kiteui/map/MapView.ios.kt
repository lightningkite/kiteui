package com.lightningkite.kiteui.map

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.core.LateInitSignal
import platform.UIKit.UIView

actual class MapView actual constructor(context: ElementContext) : NativeElement(context) {
    override val native: UIView = UIView()
    actual val api: LateInitSignal<MapApi> get() = TODO("Not yet implemented")
}