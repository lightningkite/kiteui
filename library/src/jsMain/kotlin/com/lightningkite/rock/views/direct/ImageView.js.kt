package com.lightningkite.rock.views.direct

import com.lightningkite.rock.clockMillis
import com.lightningkite.rock.models.*
import com.lightningkite.rock.navigation.PlatformNavigator
import com.lightningkite.rock.views.*
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
        addClass("viewDraws", "swapImage")
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
                setSrc(URL.createObjectURL(value.data))
            }
            is ImageResource -> {
                setSrc(PlatformNavigator.basePath + value.relativeUrl)
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
            children.forEach {
                (it as? HTMLElement)?.style?.opacity = "0"
            }
            window.setTimeout({
                children.forEach {
                    native.removeChild(it)
                }
            }, 150)
        }
        return
    }

    val newElement = document.createElement("img") as HTMLImageElement
    newElement.style.opacity = "0"
    val now = clockMillis()
    newElement.onload = label@{
        val children = (0..<native.children.length).mapNotNull { native.children[it] }
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
                native.removeChild(it)
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

@ViewDsl
actual inline fun ViewWriter.zoomableImageActual(crossinline setup: ImageView.() -> Unit) {
    // TODO
    val wrapper: ImageView.() -> Unit = {
        setup()
        scaleType = ImageScaleType.Fit
    }

    image(wrapper)
}