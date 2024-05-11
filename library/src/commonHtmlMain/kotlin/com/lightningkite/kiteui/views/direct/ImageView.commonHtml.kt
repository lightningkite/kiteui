package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import kotlin.js.JsName


actual class ImageView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("viewDraws")
        native.classes.add("swapImage")
    }


    actual var source: ImageSource? = null
        set(value) {
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
                    setSrc(value.toWeb())
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
}

@JsName("createObjectURLBlob")
expect fun createObjectURL(blob: Blob): String
@JsName("createObjectURLFileReference")
expect fun createObjectURL(fileReference: FileReference): String

expect fun RView.nativeSetSrc(url: String?)
