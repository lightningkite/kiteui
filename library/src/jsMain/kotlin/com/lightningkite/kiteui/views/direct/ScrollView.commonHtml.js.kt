package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.Listenable
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.views.RView
import org.w3c.dom.*

internal actual fun ScrollView.nativeScrollTo(
    top: Double,
    left: Double,
    animated: Boolean
) {
    native.onElement {
        (it as HTMLElement).scrollTo(ScrollToOptions(
            left = left,
            top = top,
            behavior = if(animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
        ))
    }
}
internal actual fun ScrollView.nativeScrollToElement(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
    element.native.element?.scrollIntoView(ScrollToOptions(
        behavior = if(animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
    ))
}
internal actual fun ScrollView.nativeViewport(): Rect = Rect.fromSize(
    left = native.element?.scrollLeft ?: 0.0,
    top = native.element?.scrollTop ?: 0.0,
    width = native.element?.clientWidth?.toDouble() ?: 0.0,
    height = native.element?.clientHeight?.toDouble() ?: 0.0,
)
internal actual fun ScrollView.nativeContent(): Rect =  Rect.fromSize(
    left = 0.0,
    top = 0.0,
    width = native.element?.scrollWidth?.toDouble() ?: 0.0,
    height = native.element?.scrollHeight?.toDouble() ?: 0.0,
)
internal actual fun ScrollView.nativeScrollOffset(
    x: Double, y: Double
) {
    native.onElement {
        (it as HTMLElement)
        if(x != 0.0) it.scrollLeft += x
        if(y != 0.0) it.scrollTop += y
    }
}