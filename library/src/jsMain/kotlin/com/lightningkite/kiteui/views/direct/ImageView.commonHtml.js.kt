package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.get
import org.w3c.dom.url.URL

@JsName("createObjectURLBlob")
actual fun createObjectURL(blob: Blob): String {
    return URL.Companion.createObjectURL(blob)
}

@JsName("createObjectURLFileReference")
actual fun createObjectURL(fileReference: FileReference): String {
    return URL.createObjectURL(fileReference)
}

actual fun RView.nativeSetSrc(url: String?) {
    val urlOrBlank = url ?: ""
    val element = native.element
    if(element == null){
        native.clearChildren()
        native.appendChild(FutureElement().apply {
            tag = "img"
            attributes.src = url
        })

        return
    }
    if (((element.lastElementChild as? HTMLImageElement)?.src ?: "") == urlOrBlank) {
        (element.lastElementChild as? HTMLImageElement)?.style?.opacity = "1"
        return
    }
    if (!RViewHelper.animationsEnabled) {
        element.innerHTML = ""
    }
    if (urlOrBlank.isBlank()) {
        if (RViewHelper.animationsEnabled) {
            val children = (0..<element.children.length).mapNotNull { element.children[it] }
            children.filterIsInstance<HTMLElement>().forEach {
                it.style.opacity = "0"
                window.setTimeout({
                    if (it.parentElement == element && it.style.opacity == "0") {
                        element.removeChild(it)
                    }
                }, 150)
            }
        }
        return
    }

    val newElement = document.createElement("img") as HTMLImageElement
    newElement.style.opacity = "0"
    val now = clockMillis()
    newElement.onload = label@{
//        native.style.setProperty("aspect-ratio",
//            (newElement.naturalWidth.toDouble() / newElement.naturalHeight).toString()
//        )
        val children = (0..<element.children.length).mapNotNull { element.children[it] }
        val myIndex = children.indexOf(newElement)
        if (myIndex == -1) return@label Unit
        if ((clockMillis() - now) < 32) {
            // disable animations and get it done; no reason to show the user an animation
            withoutAnimation {
                newElement.style.opacity = "1"
            }
        } else {
            newElement.style.opacity = "1"
        }
        for (index in 0..<myIndex) {
            val it = children[index] as? HTMLElement ?: continue
            window.setTimeout({
                if (it.parentElement == element) {
                    element.removeChild(it)
                }
            }, 150)
        }
    }
    newElement.src = urlOrBlank
    element.appendChild(newElement)
}