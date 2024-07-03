package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.TextOverflow
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.browser.window
import org.w3c.dom.HTMLElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.h1Actual(crossinline setup: TextView.() -> Unit): Unit = headerElement("h1", setup)

@ViewDsl
actual inline fun ViewWriter.h2Actual(crossinline setup: TextView.() -> Unit): Unit = headerElement("h2", setup)

@ViewDsl
actual inline fun ViewWriter.h3Actual(crossinline setup: TextView.() -> Unit): Unit = headerElement("h3", setup)

@ViewDsl
actual inline fun ViewWriter.h4Actual(crossinline setup: TextView.() -> Unit): Unit = headerElement("h4", setup)

@ViewDsl
actual inline fun ViewWriter.h5Actual(crossinline setup: TextView.() -> Unit): Unit = headerElement("h5", setup)

@ViewDsl
actual inline fun ViewWriter.h6Actual(crossinline setup: TextView.() -> Unit): Unit = headerElement("h6", setup)

@ViewDsl
actual inline fun ViewWriter.textActual(crossinline setup: TextView.() -> Unit): Unit = textElement("p", setup)

@ViewDsl
actual inline fun ViewWriter.subtextActual(crossinline setup: TextView.() -> Unit): Unit = textElement("span") {
    native.classList.add("subtext")
    setup()
}

actual inline var TextView.content: String
    get() = native.innerText
    set(value) {
        native.innerText = value
    }
actual inline var TextView.align: Align
    get() = when (window.getComputedStyle(native).textAlign) {
        "start" -> Align.Start
        "center" -> Align.Center
        "end" -> Align.End
        "justify" -> Align.Stretch
        else -> Align.Start
    }
    set(value) {
        native.style.textAlign = when (value) {
            Align.Start -> "start"
            Align.Center -> "center"
            Align.End -> "end"
            Align.Stretch -> "justify"
        }
    }
actual inline var TextView.textSize: Dimension
    get() = Dimension(window.getComputedStyle(native).fontSize)
    set(value) {
        native.style.fontSize = value.value
    }
actual var TextView.ellipsis: Boolean
    get() = TODO("Not yet implemented")
    set(value) {
        native.style.textOverflow = if(value) "ellipsis" else "clip"
        if(value)
            native.style.setProperty("overflow", "hidden")
        else
            native.style.removeProperty("overflow")
    }
actual var TextView.wraps: Boolean
    get() = TODO("Not yet implemented")
    set(value) {
        native.style.setProperty("text-wrap", if(value) "wrap" else "nowrap")
        println("text-wrap <- ${if(value) "wrap" else "nowrap" }")
        native.style.setProperty("text-wrap-mode", if(value) "wrap" else "nowrap")
        println("text-wrap-mode <- ${if(value) "wrap" else "nowrap" }")
    }
actual var TextView.wordBreak: WordBreak
    get() = TODO("Not yet implemented")
    set(value) {
        native.style.setProperty("word-break", if(value == WordBreak.BreakAll) "break-all" else "normal")
    }