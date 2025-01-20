package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.RView

internal actual fun ScrollingBehaviorImpl.nativeScrollTo(
    top: Double,
    left: Double,
    animated: Boolean
) {
    // no-op: we're on the server side
    // perhaps add js to scroll on boot?
}
internal actual fun ScrollingBehaviorImpl.nativeScrollToElement(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
    // no-op: we're on the server side
    // perhaps add js to scroll on boot?
}
internal actual fun ScrollingBehaviorImpl.nativeScrollOffset(
    x: Double, y: Double
) {
    // no-op: we're on the server side
    // perhaps add js to scroll on boot?
}
internal actual fun ScrollingBehaviorImpl.nativeViewport(): Rect = Rect.Zero
internal actual fun ScrollingBehaviorImpl.nativeContent(): Rect =  Rect.Zero