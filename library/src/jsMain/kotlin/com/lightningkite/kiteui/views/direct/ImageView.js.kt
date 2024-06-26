package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.basePath
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.get
import org.w3c.dom.url.URL

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NImageView = HTMLDivElement

@ViewDsl
actual inline fun ViewWriter.imageActual(crossinline setup: ImageView.() -> Unit): Unit =
    themedElement<HTMLDivElement>("div") {
        addClass("viewDraws", "kiteui-stack", "swapImage")
        setup(ImageView(this))
    }

actual inline var ImageView.source: ImageSource?
    get() = native.asDynamic().__ROCK__source as? ImageSource
    set(value) {
//        println("Setting source to $value")
        val last = native.asDynamic().__ROCK__source
        if(refreshOnParamChange && value is ImageRemote) {
            if(value.url == (last as? ImageRemote)?.url) {
//                println("Store PFQ $value")
                native.asDynamic().__ROCK__source_pfq = value
                return
            }
        } else if(value == last) {
//            println("Store PFQ $value")
            native.asDynamic().__ROCK__source_pfq = value
            return
        }
        native.asDynamic().__ROCK__source = value
        when (value) {
            null -> setSrc("")
            is ImageRemote -> {
                setSrc(value.url)
            }
            is ImageRaw -> {
                setSrc(URL.createObjectURL(value.data))
            }
            is ImageResource -> {
                setSrc(basePath + value.relativeUrl)
            }
            is ImageLocal -> {
                setSrc(URL.createObjectURL(value.file))
            }
            is ImageVector -> {
                setSrc(value.toWeb())
            }
            else -> {}
        }
    }
fun ImageView.setSrc(url: String) {
    if(!animationsEnabled) {
        native.innerHTML = ""
    }
    if(url.isBlank()) {
        if(animationsEnabled) {
            val children = (0..<native.children.length).mapNotNull { native.children[it] }
            children.filterIsInstance<HTMLElement>().forEach {
                it.style.opacity = "0"
                window.setTimeout({
                    if(it.parentElement == native && it.style.opacity == "0") {
                        native.removeChild(it)
                    }
                }, 150)
            }
        }
        return
    }

    val newElement = document.createElement("img") as HTMLImageElement
    newElement.style.opacity = "0.01"
    val now = clockMillis()
    newElement.addEventListener("error", {
        if(newElement.parentElement === native) {
            native.asDynamic().__ROCK__source = null
            (native.asDynamic().__ROCK__source_pfq as? ImageSource)?.let {
//                println("Load PFQ $it")
                native.asDynamic().__ROCK__source_pfq = null
                source = it
            }
        }
    })
    newElement.addEventListener("", {})
    newElement.onload = label@{
//        native.style.setProperty("aspect-ratio",
//            (newElement.naturalWidth.toDouble() / newElement.naturalHeight).toString()
//        )
        val children = (0..<native.children.length).mapNotNull { native.children[it] }
        val myIndex = children.indexOf(newElement)
        if(myIndex == -1) return@label Unit
        if((clockMillis() - now) < 32) {
            // disable animations and get it done; no reason to show the user an animation
            newElement.withoutAnimation {
                newElement.style.opacity = "0.8"
            }
        } else {
            newElement.style.opacity = "0.99"
        }
        for(index in 0..<myIndex) {
            val it = children[index] as? HTMLElement ?: continue
            window.setTimeout({
                if(it.parentElement == native) {
                    native.removeChild(it)
                }
            }, 150)
        }
    }
    newElement.src = url
    native.appendChild(newElement)
}
actual inline var ImageView.scaleType: ImageScaleType
    get() = TODO()
    set(value) {
        native.className = native.className.split(' ').filter { !it.startsWith("scaleType-") }.plus("scaleType-$value").joinToString(" ")
    }
actual inline var ImageView.description: String?
    get() = native.getAttribute("aria-label")
    set(value) {
        native.setAttribute("aria-label", value ?: "")
    }
actual inline var ImageView.refreshOnParamChange: Boolean
    get() = (native.asDynamic().__KITEUI__refreshOnParamChange as? Boolean) ?: false
    set(value) {
        native.asDynamic().__KITEUI__refreshOnParamChange = value
    }
actual var ImageView.naturalSize: Boolean
    get() = false
    set(value) {}

@ViewDsl
actual inline fun ViewWriter.zoomableImageActual(crossinline setup: ImageView.() -> Unit) {
    // TODO
    val wrapper: ImageView.() -> Unit = {
        setup()
        scaleType = ImageScaleType.Fit
    }

    image(wrapper)
}