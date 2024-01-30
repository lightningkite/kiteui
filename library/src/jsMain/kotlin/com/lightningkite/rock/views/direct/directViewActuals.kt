package com.lightningkite.rock.views.direct

import com.lightningkite.rock.models.*
import com.lightningkite.rock.reactive.Readable
import com.lightningkite.rock.reactive.reactiveScope
import com.lightningkite.rock.views.*
import kotlinx.browser.document
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.pointerevents.PointerEvent

fun ViewWriter.todo(name: String) = element<HTMLSpanElement>("span") {
    innerText = name
}

inline fun <T : HTMLElement> ViewWriter.themedElementEditable(name: String, crossinline setup: T.() -> Unit) = themedElement<T>(name) {
    classList.add("editable")
    setup(this)
}

inline fun <T : HTMLElement> ViewWriter.themedElementClickable(name: String, crossinline setup: T.() -> Unit) = themedElement<T>(name) {
    classList.add("clickable")
    setup(this)
}

inline fun <T : HTMLElement> ViewWriter.themedElement(name: String, viewDraws: Boolean = true, crossinline setup: T.() -> Unit) {
    element<T>(name) {
        handleTheme(this, viewDraws = viewDraws)
        setup(this)
    }
}

inline fun <T : HTMLElement> ViewWriter.themedElementBackIfChanged(name: String, crossinline setup: T.() -> Unit) = themedElement<T>(name, viewDraws = false, setup)

@ViewDsl
internal fun ViewWriter.textElement(elementBase: String, setup: TextView.() -> Unit): Unit =
    element<HTMLDivElement>(elementBase) {
        handleTheme(this, true)
        setup(TextView(this))
        style.whiteSpace = "pre-wrap"
    }

@ViewDsl
internal fun ViewWriter.headerElement(elementBase: String, setup: TextView.() -> Unit): Unit =
    element<HTMLDivElement>(elementBase) {
        classList.add("title")
        handleTheme(this, true)
        setup(TextView(this))
        style.whiteSpace = "pre-wrap"
    }

fun HTMLElement.__resetContentToOptionList(options: List<WidgetOption>, selected: String) {
    innerHTML = ""
    for (item in options) appendChild((document.createElement("option") as HTMLOptionElement).apply {
        this.value = item.key
        this.innerText = item.display
        this.selected = item.key == selected
    })
}

internal fun Canvas.pointerListenerHandler(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): (Event) -> Unit =
    {
        val event = it as PointerEvent
        val b = native.getBoundingClientRect()
        action(event.pointerId, event.pageX - b.x, event.pageY - b.y, b.width, b.height)
    }


external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver)->Unit) {
    fun disconnect()
    fun observe(target: Element, options: ResizeObserverOptions = definedExternally)
    fun unobserve(target: Element)
}
external interface ResizeObserverOptions {
    val box: String
}
external interface ResizeObserverEntry {
    val target: Element
    val contentRect: DOMRectReadOnly
    val contentBoxSize: ResizeObserverEntryBoxSize
    val borderBoxSize: ResizeObserverEntryBoxSize
}
external interface ResizeObserverEntryBoxSize {
    val blockSize: Double
    val inlineSize: Double
}

data class SizeReader(val native: HTMLElement, val key: String): Readable<Double> {
    override suspend fun awaitRaw(): Double = native.asDynamic()[key].unsafeCast<Int>().toDouble()
    override fun addListener(listener: () -> Unit): () -> Unit {
        val o = ResizeObserver { _, _ ->
            listener()
        }
        o.observe(native)
        return { o.disconnect() }
    }
}