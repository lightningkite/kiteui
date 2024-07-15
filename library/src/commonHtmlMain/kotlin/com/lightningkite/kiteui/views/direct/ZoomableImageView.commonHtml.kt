package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import kotlin.js.JsName


actual class ZoomableImageView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("viewDraws")
        native.classes.add("kiteui-stack")
    }


    actual var source: ImageSource? = null
        set(value) {
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (field as? ImageRemote)?.url) return
            } else if (value == field) return
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
    actual var refreshOnParamChange: Boolean = false
}

