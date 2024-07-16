package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Align
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.createElement
import kotlinx.dom.hasClass
import kotlinx.dom.removeClass
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import org.w3c.dom.svg.SVGElement
import kotlin.js.Json
import kotlin.js.json
import kotlin.random.Random

actual class FutureElement actual constructor() {
    val elementToDo = ArrayList<(Element) -> Unit>()
    var element: Element? = null
        set(value) {
            id = value?.id
            content = value?.innerHTML?.takeUnless { it.isBlank() }
            field = value
            style.native = (value as? HTMLElement)?.style ?: (value as? SVGElement)?.style
            attributes.native = value
        }

    inline fun onElement(crossinline action: (Element) -> Unit) {
        element?.let(action) ?: elementToDo.add { action(it) }
    }

    fun create(): Element {
        element?.let { return it }
        val e = xmlns?.let { document.createElementNS(it, tag) } ?: document.createElement(tag)
        objectAssign(e, attributesBack)
        (e as? HTMLElement)?.style?.let {
            objectAssign(it, styleBack)
            futureStyles.forEach { (k, v) -> it.setProperty(k, v) }
        } ?: (e as? SVGElement)?.style?.let {
            objectAssign(it, styleBack)
            futureStyles.forEach { (k, v) -> it.setProperty(k, v) }
        }
        futureAttributes.forEach {
            e.setAttribute(it.key, it.value)
        }
        content?.let { (e as? HTMLElement)?.innerText = it }
        children.forEach {
            e.appendChild(it.create())
        }
        (e as? HTMLElement)?.let {
            e.className = classes.joinToString(" ")
        } ?: run {
            e.classList.add(*classes.toTypedArray())
        }
        objectAssign(e, eventsBack)
        element = e
        elementToDo.forEach { it(e) }
        return e
    }

    actual fun click() { onElement { (it as HTMLElement).click() } }
    actual fun focus() { onElement { (it as HTMLElement).focus() } }
    actual fun blur() { onElement { (it as HTMLElement).blur() } }

    actual var xmlns: String? = null
    actual var tag: String = "tag"
    val attributesBack = json()
    actual val attributes: FutureElementAttributes = FutureElementAttributes(attributesBack)
    val styleBack = json()
    actual val style: FutureElementStyle = FutureElementStyle(styleBack)
    val eventsBack = json()
    actual inline fun addEventListener(
        name: String,
        crossinline listener: (Event) -> Unit
    ) {
        element?.addEventListener(name, { it:Event -> listener(it) }) ?: run {
            eventsBack["on$name"] = { it:Event -> listener(it) }
        }
    }
    val futureStyles = HashMap<String, String>()
    actual fun setStyleProperty(key: String, value: String?) {
        val element = element
        if(element == null) {
            if(value == null) {
                futureStyles.remove(key)
            } else {
                futureStyles.put(key, value)
            }
        } else {
            val style = (element as? HTMLElement)?.style ?: (element as? SVGElement)?.style ?: return
            if(value == null) {
                style.removeProperty(key)
            } else {
                style.setProperty(key, value)
            }
        }
    }
    val futureAttributes = HashMap<String, String>()
    actual fun setAttribute(key: String, value: String?) {
        val element = element
        if(element == null) {
            if(value == null) {
                futureAttributes.remove(key)
            } else {
                futureAttributes.put(key, value)
            }
        } else {
            if(value == null) {
                element?.removeAttribute(key)
            } else {
                element?.setAttribute(key, value)
            }
        }
    }


    actual var classes: MutableSet<String> = ClassSet()
    actual var id: String? = null
        set(value) {
            field = value
            element?.id = value ?: Random.nextInt().toString()
        }
    actual var content: String? = null
        set(value) {
            field = value
            value?.let {
                (element as? HTMLElement)?.innerText = value
            }
        }
    private val lastChildren = ArrayList<FutureElement>()
    actual val children: List<FutureElement>
        get() {
            return element?.let {
                it.children.let {
                    (0..<it.length).map { i ->
                        val item = it.item(i)
                        lastChildren.find { it.element === item }
                            ?: FutureElement().apply { element = it.item(i) }
                    }
                }.also { lastChildren.clear(); lastChildren.addAll(it) }
            } ?: lastChildren
        }

    actual fun appendChild(element: FutureElement) {
        lastChildren.add(element)
        this.element?.let {
            it.appendChild(element.create())
        }
    }
    actual fun appendChild(index: Int, element: FutureElement) {
        if (index <= lastChildren.size) lastChildren.add(index, element)
        this.element?.let {
            it.children.item(index)?.let { before ->
                it.insertBefore(element.create(), before)
            } ?: it.appendChild(element.create())
        }
    }

    actual fun removeChild(index: Int) {
        element?.let {
            it.children.item(index)?.let { v -> it.removeChild(v) }
        } ?: lastChildren.removeAt(index)
    }

    actual fun clearChildren() {
        lastChildren.clear()
        this.element?.innerHTML = ""
    }

    inner class ClassSet : MutableSet<String> {
        val map = HashSet<String>()
        override fun add(element: String): Boolean = this@FutureElement.element?.addClass(element) ?: map.add(element)
        override fun addAll(elements: Collection<String>): Boolean =
            this@FutureElement.element?.addClass(*elements.toTypedArray()) ?: map.addAll(elements)

        override val size: Int get() = this@FutureElement.element?.classList?.length ?: map.size
        override fun clear() = element?.let { it.className = "" } ?: map.clear()
        override fun isEmpty(): Boolean = element?.className?.isBlank() ?: map.isEmpty()
        override fun containsAll(elements: Collection<String>): Boolean = elements.all { contains(it) }
        override fun contains(element: String): Boolean =
            this@FutureElement.element?.let { it.hasClass(element) } ?: map.contains(element)

        override fun iterator(): MutableIterator<String> = this@FutureElement.element?.let {
            var index = 0
            val list = it.classList
            object : MutableIterator<String> {
                override fun hasNext(): Boolean = index < list.length
                lateinit var last: String
                override fun next(): String {
                    index++
                    last = list.item(index) ?: ""
                    return last
                }

                override fun remove() {
                    list.remove(last)
                    index--
                }
            }
        } ?: map.iterator()

        override fun retainAll(elements: Collection<String>): Boolean = throw NotImplementedError()
        override fun remove(element: String): Boolean =
            this@FutureElement.element?.removeClass(element) ?: map.remove(element)

        override fun removeAll(elements: Collection<String>): Boolean =
            this@FutureElement.element?.removeClass(*elements.toTypedArray()) ?: map.removeAll(elements)
    }
}

actual class FutureElementStyle(var native: dynamic)
actual class FutureElementAttributes(var native: dynamic)

actual fun RView.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
}

inline fun objectAssign(target: dynamic, source: dynamic) = js("Object.assign(target, source)")