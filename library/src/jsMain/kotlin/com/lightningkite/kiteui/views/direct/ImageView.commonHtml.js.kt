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

private var FutureElement.loadNum: Int
    get() = asDynamic().__loadNum as? Int ?: 0
    set(value) {
        asDynamic().__loadNum = value
    }

actual fun RView.nativeSetSrc(url: String?, onSuccess: () -> Unit, onError: () -> Unit) {
    val animating = animationsEnabled
    val urlOrBlank = url ?: ""
    val element = native.element
    val loadNum = ++native.loadNum
    native.onElement { element ->

        if (((element.lastElementChild as? HTMLImageElement)?.src ?: "") == urlOrBlank) {
            (element.lastElementChild as? HTMLImageElement)?.style?.opacity = "1"
            return@onElement
        }
        if (!animating) {
            element.innerHTML = ""
        }
        if (urlOrBlank.isBlank()) {
            if (animating) {
                val children = (0..<element.children.length).mapNotNull { element.children[it] }
                children.filterIsInstance<HTMLElement>().forEach {
                    it.style.opacity = "0"
                    window.setTimeout({
                        if (it.parentElement == element) {
                            element.removeChild(it)
                        }
                    }, theme.transitionDuration.inWholeMilliseconds.toInt())
                }
            }
            return@onElement
        }

        val newElement = document.createElement("img") as HTMLImageElement
        newElement.style.opacity = "0"
        val now = clockMillis()
        window.setTimeout({
            println("window.setTimeout({   -  ${loadNum} == ${native.loadNum}")
            if (loadNum == native.loadNum) {
                native.classes.add("loading")
            }
        }, 16)
        newElement.onerror = { dyn, msg, a, b, c ->
            println("newElement.onerror = { dyn, msg, a, b, c ->   -  ${loadNum} == ${native.loadNum}")
            if (loadNum == native.loadNum) {
                native.classes.remove("loading")
                native.loadNum++
            }
            onError()
        }
        newElement.onload = label@{
            println("newElement.onload = label@{   -  ${loadNum} == ${native.loadNum}")
            if (loadNum == native.loadNum) {
                native.classes.remove("loading")
                native.loadNum++
            }
            onSuccess()
//        native.style.setProperty("aspect-ratio",
//            (newElement.naturalWidth.toDouble() / newElement.naturalHeight).toString()
//        )
            val children = (0..<element.children.length).mapNotNull { element.children[it] }
            val myIndex = children.indexOf(newElement)
            if (myIndex == -1) return@label Unit
            if ((clockMillis() - now).also { println("Diff is ${it}") } < 32) {
                // disable animations and get it done; no reason to show the user an animation
                newElement.withoutAnimation {
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
                }, theme.transitionDuration.inWholeMilliseconds.toInt())
            }
        }
        newElement.src = urlOrBlank
        element.appendChild(newElement)
    }
}