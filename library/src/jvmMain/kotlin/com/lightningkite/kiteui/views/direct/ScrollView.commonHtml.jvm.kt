package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.Readable

internal actual fun ScrollView.nativeScrollTo(
    top: Double,
    left: Double,
    animated: Boolean
) {
    // no-op: we're on the server side
    // perhaps add js to scroll on boot?
}
internal actual fun ScrollView.nativeViewport(): Rect = Rect.Zero
internal actual fun ScrollView.nativeContent(): Rect =  Rect.Zero