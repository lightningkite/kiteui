package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.jsArrayOf
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.PlatformNavigator
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
actual class NImageView(override val js: HTMLDivElement): NView2<HTMLDivElement>()

@ViewDsl
actual inline fun ViewWriter.imageActual(crossinline setup: ImageView.() -> Unit): Unit =
    themedElement("div", ::NImageView) {
        js.addClass("viewDraws", "swapImage")
        setup(ImageView(this))
    }

actual inline var ImageView.source: ImageSource?
    get() = TODO()
    set(value) {
        when (value) {
            null -> setSrc("")
            is ImageRemote -> {
                setSrc(value.url)
            }
            is ImageRaw -> {
                TODO()
//                setSrc(URL.createObjectURL(Blob(jsArrayOf(value.data))))
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
        native.js.innerHTML = ""
    }
    if(url.isBlank()) {
        if(animationsEnabled) {
            val children = (0..<native.js.children.length).mapNotNull { native.js.children[it] }
            children.forEach {
                (it as? HTMLElement)?.style?.opacity = "0"
            }
            window.setTimeout({
                children.forEach {
                    native.js.removeChild(it)
                }
                null
            }, 150)
        }
        return
    }

    val newElement = document.createElement("img") as HTMLImageElement
    newElement.style.opacity = "0"
    val now = clockMillis()
    newElement.onload = label@{
        val children = (0..<native.js.children.length).mapNotNull { native.js.children[it] }
        val myIndex = children.indexOf(newElement)
        if(myIndex == -1) return@label Unit
        if((clockMillis() - now) < 32) {
            // disable animations and get it done; no reason to show the user an animation
            newElement.withoutAnimation {
                newElement.style.opacity = "1"
            }
        } else {
            newElement.style.opacity = "1"
        }
        for(index in 0..<myIndex) {
            val it = children[index]
            window.setTimeout({
                native.js.removeChild(it)
            }, 150)
        }
    }
    newElement.src = url
    native.js.appendChild(newElement)
}
actual inline var ImageView.scaleType: ImageScaleType
    get() = TODO()
    set(value) {
        native.js.className = native.js.className.split(' ').filter { !it.startsWith("scaleType-") }.plus("scaleType-$value").joinToString(" ")
    }
actual inline var ImageView.description: String?
    get() = native.js.getAttribute("aria-label")
    set(value) {
        native.js.setAttribute("aria-label", value ?: "")
    }

@ViewDsl
actual inline fun ViewWriter.zoomableImageActual(crossinline setup: ImageView.() -> Unit) {
    // TODO
    val wrapper: ImageView.() -> Unit = {
        setup()
        scaleType = ImageScaleType.Fit
    }

    image(wrapper)
}