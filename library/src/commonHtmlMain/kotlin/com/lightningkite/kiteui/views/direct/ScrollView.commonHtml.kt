package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

actual class ScrollView actual constructor(
    context: RContext,
    actual val horizontal: Boolean,
    actual val vertical: Boolean
) : RView(context) {
    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
        if(horizontal) {
            native.classes += "scroll-horizontal"
            native.style.overflowX = "auto"
        } else {
            native.style.overflowX = "none"
        }
        if(vertical) {
            native.classes += "scroll-vertical"
            native.style.overflowY = "auto"
        } else {
            native.style.overflowY = "none"
        }
    }
    actual var showScrollBars: Boolean = true
        set(value) {
            field = value
            if(value) native.classes -= "hideScrollbar"
            else native.classes += "hideScrollbar"
        }
    actual val scrollReason: Readable<ScrollReason>
        get() = TODO("Not yet implemented")

    override fun internalAddChild(index: Int, view: RView) {
        if(!horizontal) {
            view.native.style.width = "100%"
        }
        if(!vertical) {
            view.native.style.height = "100%"
        }
        super.internalAddChild(index, view)
    }

    private val rs = native.resizeObserver()
    actual val viewport: Readable<Rect> = (native.vevent("scroll") + rs).lens { nativeViewport() }
    actual val content: Readable<Rect> = rs.lens { nativeContent() }
    actual fun scrollTo(top: Double, left: Double, animated: Boolean) = nativeScrollTo(top, left, animated)
}
internal expect fun ScrollView.nativeScrollTo(top: Double, left: Double, animated: Boolean)
internal expect fun ScrollView.nativeViewport(): Rect
internal expect fun ScrollView.nativeContent(): Rect