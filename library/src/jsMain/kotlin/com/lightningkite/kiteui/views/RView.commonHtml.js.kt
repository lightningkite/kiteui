package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.px
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.hasClass
import kotlinx.dom.removeClass
import org.w3c.dom.*
import org.w3c.dom.svg.SVGElement
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.roundToInt
import kotlin.random.Random

actual class FutureElement actual constructor() {
    actual val actualElementForLeakTracking: Any? get() = element
    val elementToDo = ArrayList<(Element) -> Unit>()
    var element: Element? = null
        private set(value) {
            field = value
            style.native = (value as? HTMLElement)?.style ?: (value as? SVGElement)?.style
            attributes.native = value
        }

    fun hydrate(value: Element) {
        id = value.id
        content = value.innerHTML.takeUnless { it.isBlank() }
        this.element = value
    }

    inline fun onElement(crossinline action: (Element) -> Unit) {
        element?.let(action) ?: elementToDo.add { action(it) }
    }

    fun create(): Element {
        element?.let { return it }
        val e = xmlns?.let { document.createElementNS(it, tag) } ?: document.createElement(tag)
        id?.let { e.id = it }
        objectAssign(e, attributesBack)
        (e as? HTMLElement)?.style?.let {
            objectAssign(it, styleBack)
            forEach(futureStyles) { k, v -> it.setProperty(k, v) }
        } ?: (e as? SVGElement)?.style?.let {
            objectAssign(it, styleBack)
            forEach(futureStyles) { k, v -> it.setProperty(k, v) }
        }
        forEach(futureAttributes) { k, v ->
            e.setAttribute(k, v)
        }
        content?.let { (e as? HTMLElement)?.innerText = it }
        innerHtmlUnsafe?.let { (e as? HTMLElement)?.innerHTML = it }
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

    actual fun click() {
        onElement { (it as HTMLElement).click() }
    }

    actual fun focus() {
        onElement { (it as HTMLElement).focus() }
    }

    actual fun blur() {
        onElement { (it as HTMLElement).blur() }
    }

    actual fun screenRectangle(): Rect? {
        return element?.getBoundingClientRect()?.let {
            Rect(
                left = it.left,
                right = it.right,
                top = it.top,
                bottom = it.bottom,
            )
        }
    }

    actual var xmlns: String? = null
    actual var tag: String = "tag"
    val attributesBack = json()
    actual val attributes: FutureElementAttributes = FutureElementAttributes(attributesBack)
    val styleBack = json()
    actual val style: FutureElementStyle = FutureElementStyle(styleBack)
    actual var desiredVerticalGravity: Align? = null
    actual var desiredHorizontalGravity: Align? = null
    val eventsBack = json()
    actual inline fun addEventListener(
        name: String,
        crossinline listener: (Event) -> Unit
    ) {
        element?.addEventListener(name, { it: Event -> listener(it) }) ?: run {
            @Suppress("UNCHECKED_CAST") val old = eventsBack["on$name"] as? (Event) -> Unit
            eventsBack["on$name"] = { it: Event -> old?.invoke(it); listener(it) }
        }
    }

    actual inline fun replaceEventListener(
        name: String,
        crossinline listener: (Event) -> Unit
    ) {
        element?.let { it.asDynamic()["on$name"] = { it: Event -> listener(it) } } ?: run {
            eventsBack["on$name"] = { it: Event -> listener(it) }
        }
    }

    val futureStyles = json()
    actual fun setStyleProperty(key: String, value: String?) {
        val element = element
        if (element == null) {
            if (value == null) {
                remove(futureStyles, key)
//                futureStyles.set(key, null)
            } else {
                futureStyles.set(key, value)
            }
        } else {
            val style = (element as? HTMLElement)?.style ?: (element as? SVGElement)?.style ?: return
            if (value == null) {
                style.removeProperty(key)
            } else {
                style.setProperty(key, value)
            }
        }
    }

    val futureAttributes = json()
    actual fun setAttribute(key: String, value: String?) {
        val element = element
        if (element == null) {
            if (value == null) {
                remove(futureAttributes, key)
//                futureAttributes.set(key, null)
            } else {
                futureAttributes.set(key, value)
            }
        } else {
            if (value == null) {
                element.removeAttribute(key)
            } else {
                element.setAttribute(key, value)
            }
        }
    }


    actual var classes: MutableSet<String> = ClassSet()
    actual inline fun flushClasses() {}
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
    actual var innerHtmlUnsafe: String? = null
        set(value) {
            field = value
            value?.let {
                (element as? HTMLElement)?.innerHTML = value
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
        assertSizeMatch()
        lastChildren.add(element)
        this.element?.let {
            it.appendChild(element.create())
        }
        assertSizeMatch()
    }

    actual fun appendChild(index: Int, element: FutureElement) {
        assertSizeMatch()
        if (index > lastChildren.size) throw IllegalStateException()
        lastChildren.add(index, element)
        this.element?.let {
            it.children.item(index)?.let { before ->
                it.insertBefore(element.create(), before)
            } ?: it.appendChild(element.create())
        }
        assertSizeMatch()
    }

    actual fun removeChild(index: Int) {
        assertSizeMatch()
        lastChildren.removeAt(index)
        element?.let {
            it.children.item(index)?.let { v -> it.removeChild(v) }
        }
        assertSizeMatch()
    }

    actual fun clearChildren() {
        assertSizeMatch()
        lastChildren.clear()
        this.element?.innerHTML = ""
        assertSizeMatch()
    }

    private fun assertSizeMatch() {
        this.element?.let {
            if (it.childElementCount != lastChildren.size) throw IllegalStateException("Size mismatch - ${it.childElementCount} vs ${lastChildren.size}")
            lastChildren.forEachIndexed { index, child ->
                if (child.element != it.children.item(index)) console.warn(
                    "WARNING: Child order inconsistency at index $index",
                    it
                )
            }
        }
        // assert order
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
                    last = list.item(index) ?: ""
                    index++
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

private fun forEach(receiver: Json, action: (key: String, value: dynamic) -> Unit) {
    for (key in js("Object.keys(receiver)")) {
        action(key, receiver[key])
    }
}

private fun remove(receiver: Json, key: String) {
    js("delete receiver[key]")
}

actual class FutureElementStyle(var native: dynamic)
actual class FutureElementAttributes(var native: dynamic)

fun Align?.logicalPosition(): ScrollLogicalPosition = when (this) {
    Align.Start -> ScrollLogicalPosition.START
    Align.Center -> ScrollLogicalPosition.CENTER
    Align.End -> ScrollLogicalPosition.END

    Align.Stretch -> ScrollLogicalPosition.START
    null -> ScrollLogicalPosition.NEAREST
}

actual fun RView.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
    native.element?.scrollIntoView(
        ScrollIntoViewOptions(
            block = vertical.logicalPosition(),
            inline = horizontal.logicalPosition(),
            behavior = if (animate) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
        )
    )
}

@Suppress("NOTHING_TO_INLINE")
inline fun objectAssign(target: dynamic, source: dynamic) = js("Object.assign(target, source)")
actual fun RView.nativeSetDragData(data: DragData?) {
    native.onElement { element ->
        if (data != null) {
            (element as HTMLElement).ondragstart = { event ->
                event.stopPropagation()
                for ((type, value) in data.typeToData) {
                    event.dataTransfer!!.setData(type, value)
                    data.dragShadow?.let { shadow ->
                        shadow.view.native.onElement {
                            event.dataTransfer!!.setDragImage(it, shadow.xOffset?.px?.roundToInt() ?: 0, shadow.yOffset?.px?.roundToInt() ?: 0)
                        }
                    }
                }
            }
        } else {
            (element as HTMLElement).ondragstart = null
        }
    }
}


actual fun RView.nativeOnDrop(listener: DropTargetDelegate?) {
    fun DragEvent.toDragEvent() = com.lightningkite.kiteui.models.DragEvent(
        data = DragData("", typeToData = dataTransfer!!.types.associate { it to dataTransfer!!.getData(it) }),
        xInView = x,
        yInView = y
    )

    native.onElement {
        if (listener != null) {
            it as HTMLElement
            it.ondragover = { e ->
                if (listener.over(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondragenter = { e ->
                if (listener.enter(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondragleave = { e ->
                if (listener.exit(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondragend = { e ->
                if (listener.end(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
            it.ondrop = { e ->
                if (listener.drop(e.toDragEvent())) {
                    e.preventDefault()
                    e.stopPropagation()
                }
            }
        } else {
            (it as HTMLElement).ondragover = null
            it.ondragleave = null
            it.ondragexit = null
            it.ondrop = null
        }
    }
}