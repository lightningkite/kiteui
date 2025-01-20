package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.views.RView
import kotlinx.browser.window
import org.w3c.dom.*

internal actual fun ScrollingBehaviorImpl.nativeScrollTo(
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
internal actual fun ScrollingBehaviorImpl.nativeScrollToElement(element: RView, horizontal: Align, vertical: Align, animated: Boolean) {
    element.native.element?.scrollIntoView(ScrollToOptions(
        behavior = if(animated) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
    ))
}
internal actual fun ScrollingBehaviorImpl.nativeViewport(): Rect = Rect.fromSize(
    left = native.element?.scrollLeft ?: 0.0,
    top = native.element?.scrollTop ?: 0.0,
    width = native.element?.clientWidth?.toDouble() ?: 0.0,
    height = native.element?.clientHeight?.toDouble() ?: 0.0,
)
internal actual fun ScrollingBehaviorImpl.nativeContent(): Rect =  Rect.fromSize(
    left = 0.0,
    top = 0.0,
    width = native.element?.scrollWidth?.toDouble() ?: 0.0,
    height = native.element?.scrollHeight?.toDouble() ?: 0.0,
)
internal actual fun ScrollingBehaviorImpl.nativeScrollOffset(
    x: Double, y: Double
) {
    native.onElement {
        (it as HTMLElement)
        val sdn = it.sdnUp()
        // behold joseph's "safari is a bitch" protection
        val expectedLeft = it.scrollLeft + x
        val expectedTop = it.scrollTop + y
        var trySet: () -> Unit = {}
        trySet = {
            it.scrollLeft = expectedLeft
            it.scrollTop = expectedTop
//            window.setTimeout({
//                if (it.sdn() == sdn) {
//                    if (it.scrollTop != expectedTop || it.scrollLeft != expectedLeft) {
//                        println("Safari, why? ${it.scrollTop} != ${expectedTop} || ${it.scrollLeft} != ${expectedLeft}")
//                        trySet()
//                    }
//                }
//            })
        }
        trySet()
    }
}

private fun HTMLElement.sdnUp(): Int {
    val it = this
    val sdn = sdn()
    it.asDynamic().__scrollDirectiveNumber = sdn
    return sdn
}

private fun HTMLElement.sdn() = (asDynamic().__scrollDirectiveNumber as? Int ?: 0)