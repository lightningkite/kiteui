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
                    setSrc(DynamicCss.basePath + value.relativeUrl)
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
    actual var description: String?
        get() = native.attributes["aria-label"]
        set(value) {
            native.attributes["aria-label"] = value
        }
}

@JsName("createObjectURLBlob")
expect fun createObjectURL(blob: Blob): String
@JsName("createObjectURLFileReference")
expect fun createObjectURL(fileReference: FileReference): String

expect fun ImageView.nativeSetSrc(url: String?)
//{
//    if (((native.lastElementChild as? HTMLImageElement)?.src ?: "") == url) {
//        (native.lastElementChild as? HTMLImageElement)?.style?.opacity = "1"
//        return
//    }
//    if (!RViewHelper.animationsEnabled) {
//        native.innerHTML = ""
//    }
//    if (url.isBlank()) {
//        if (RViewHelper.animationsEnabled) {
//            val children = (0..<native.children.length).mapNotNull { native.children[it] }
//            children.filterIsInstance<HTMLElement>().forEach {
//                it.style.opacity = "0"
//                window.setTimeout({
//                    if (it.parentElement == native && it.style.opacity == "0") {
//                        native.removeChild(it)
//                    }
//                }, 150)
//            }
//        }
//        return
//    }
//
//    val newElement = document.createElement("img") as HTMLImageElement
//    newElement.style.opacity = "0"
//    val now = clockMillis()
//    newElement.onload = label@{
////        native.style.setProperty("aspect-ratio",
////            (newElement.naturalWidth.toDouble() / newElement.naturalHeight).toString()
////        )
//        val children = (0..<native.children.length).mapNotNull { native.children[it] }
//        val myIndex = children.indexOf(newElement)
//        if (myIndex == -1) return@label Unit
//        if ((clockMillis() - now) < 32) {
//            // disable animations and get it done; no reason to show the user an animation
//            newElement.withoutAnimation {
//                newElement.style.opacity = "1"
//            }
//        } else {
//            newElement.style.opacity = "1"
//        }
//        for (index in 0..<myIndex) {
//            val it = children[index] as? HTMLElement ?: continue
//            window.setTimeout({
//                if (it.parentElement == native && it.style.opacity == "0") {
//                    native.removeChild(it)
//                }
//            }, 150)
//        }
//    }
//    newElement.src = url
//    native.appendChild(newElement)
//}