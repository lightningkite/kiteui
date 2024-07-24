package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import kotlin.js.JsName


actual class ImageView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("viewDraws")
        native.classes.add("swapImage")
    }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Stack.internalAddChildStack(this, index, view)
    }

    actual var source: ImageSource? = null
        set(value) {
            if(refreshOnParamChange && value is ImageRemote) {
                if(value.url == (field as? ImageRemote)?.url) return
            } else if(value == field) return
            field = value
            when (value) {
                null -> setSrc("")
                is ImageRemote -> {
                    setSrc(value.url)
                }

                is ImageRaw -> {
                    setSrc(createObjectURL(value.data))
                }

                is ImageResource -> {
                    setSrc(context.basePath + value.relativeUrl)
                }

                is ImageLocal -> {
                    setSrc(createObjectURL(value.file))
                }

                is ImageVector -> {
                    setSrc(value.vectorToSvgDataUrl())
                }

                else -> {}
            }
        }

    fun setSrc(url: String) = nativeSetSrc(url)

    actual var scaleType: ImageScaleType = ImageScaleType.Fit
        set(value) {
            field = value
            native.classes.removeAll { it.startsWith("scaleType-") }
            native.classes.add("scaleType-$value")
        }
    actual var description: String? = null
        set(value) {
            field = value
            native.setAttribute("aria-label", value)
        }
    actual var refreshOnParamChange: Boolean = false

    /**
     * When true, images are dimensioned according to the platform logical coordinate space as opposed to the physical
     * coordinate space. This will cause images to appear closer to their natural size on supported platforms with high
     * density screens.
     */
    actual var naturalSize: Boolean = false
}

@JsName("createObjectURLBlob")
expect fun createObjectURL(blob: Blob): String
@JsName("createObjectURLFileReference")
expect fun createObjectURL(fileReference: FileReference): String

expect fun RView.nativeSetSrc(url: String?)
