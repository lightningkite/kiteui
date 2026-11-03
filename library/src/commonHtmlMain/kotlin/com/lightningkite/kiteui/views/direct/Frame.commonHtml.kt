package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.views.*

public actual class Frame actual constructor(context: ElementContext) : NativeContainerElement(context) {
    init {
        native.tag = "div"
        native.style.lineHeight = "0px !important"
    }

    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    public companion object {
        internal fun internalAddChildStack(on: NativeContainerElement, index: Int, element: Element) {
            val elementNative = element.underlyingNativeElement.native

            if (index == 0) {
                on.native.style.display = "flex"
            } else if (index >= 1) {
                if(on.native.style.display != "grid") {
                    on.native.style.display = "grid"
                    on.native.setStyleProperty("grid-template-columns", "100%")
                    on.native.setStyleProperty("grid-template-rows", "100%")
                }
            }
            on.native.children.forEachIndexed { index, futureElement ->
                futureElement.style.zIndex = index.toString()
            }

            elementNative.setStyleProperty("grid-area", "1 / 1 / 1 / 1")
            when(elementNative.desiredVerticalGravity) {
                Align.Start -> {
                    elementNative.style.verticalAlign = "top"
                    elementNative.style.alignSelf = "start"
                }
                Align.Center -> {
                    if(on.native.style.display != "grid") {
                        on.native.style.display = "grid"
                        on.native.setStyleProperty("grid-template-columns", "100%")
                        on.native.setStyleProperty("grid-template-rows", "100%")
                    }
                    elementNative.style.alignSelf = "center"
                }
                Align.End -> {
                    elementNative.style.verticalAlign = "bottom"
                    elementNative.style.alignSelf = "end"
                }
                else -> {
                    elementNative.style.verticalAlign = "top"
                    if(elementNative.style.height.isNullOrEmpty()) elementNative.style.height = "100%"
                    elementNative.style.alignSelf = "stretch"
                }
            }
            when(elementNative.desiredHorizontalGravity) {
                Align.Start -> {
                    elementNative.style.marginLeft = "unset"
                    elementNative.style.marginRight = "auto"
                    elementNative.style.justifySelf = "start"
                }
                Align.Center -> {
                    elementNative.style.marginLeft = "auto"
                    elementNative.style.marginRight = "auto"
                    elementNative.style.justifySelf = "center"
                }
                Align.End -> {
                    elementNative.style.marginLeft = "auto"
                    elementNative.style.marginRight = "unset"
                    elementNative.style.justifySelf = "end"
                }
                else -> {
                    if(elementNative.style.width.isNullOrEmpty()) elementNative.style.width = "100%"
                    elementNative.style.justifySelf = "stretch"
                }
            }
        }
    }
}
