package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import kotlinx.dom.addClass
import kotlinx.dom.appendElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.get
import org.w3c.dom.svg.SVGSVGElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NIconView = HTMLDivElement

@ViewDsl
actual inline fun ViewWriter.iconActual(crossinline setup: IconView.() -> Unit): Unit =
    themedElement<NIconView>("div") {
        this.setAttribute("role", "img")
        addClass("viewDraws")
        addClass("icon")
        setup(IconView(this))
    }

actual inline var IconView.source: Icon?
    get() = native.asDynamic().__ROCK__icon as? Icon
    set(value) {
        native.asDynamic().__ROCK__icon = value
        native.innerHTML = ""
        value?.let {
            native.renderSvgIcon(value)
        }
    }

actual inline var IconView.description: String?
    get() = (native.firstElementChild as SVGSVGElement).getElementsByTagName("title").get(0)?.innerHTML
    set(value) {
        (native.firstElementChild as? SVGSVGElement)?.getElementsByTagName("title")?.get(0)?.let {
            it.innerHTML = value ?: ""
        } ?: (native.firstElementChild as? SVGSVGElement)?.let {
            it.appendElement("title") {
                innerHTML = value ?: ""
            }
        }
    }
