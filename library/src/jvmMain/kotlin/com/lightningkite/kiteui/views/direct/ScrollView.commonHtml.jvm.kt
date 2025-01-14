package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.RView

internal actual fun ScrollView.nativeScrollTo(
    top: Double,
    left: Double,
    animated: Boolean
) {
    // no-op: we're on the server side
    // perhaps add js to scroll on boot?
}
internal actual fun ScrollView.nativeScrollToElement(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
    // no-op: we're on the server side
    // perhaps add js to scroll on boot?
}
internal actual fun ScrollView.nativeScrollOffset(
    x: Double, y: Double
) {
    // no-op: we're on the server side
    // perhaps add js to scroll on boot?
}
internal actual fun ScrollView.nativeViewport(): Rect = Rect.Zero
internal actual fun ScrollView.nativeContent(): Rect =  Rect.Zero