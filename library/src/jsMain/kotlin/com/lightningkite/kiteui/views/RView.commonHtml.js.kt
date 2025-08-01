package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DragEvent
import com.lightningkite.kiteui.models.Rect
import kotlinx.browser.document
import kotlinx.dom.addClass
import kotlinx.dom.hasClass
import kotlinx.dom.removeClass
import org.w3c.dom.*
import org.w3c.dom.svg.SVGElement
import kotlin.js.Json
import kotlin.js.json
import kotlin.random.Random

public actual class FutureElement public actual constructor() {
    public actual val actualElementForLeakTracking: Any? get() = element
    public val elementToDo: ArrayList<(Element) -> Unit> = ArrayList<(Element) -> Unit>()
    public var element: Element? = null
        private set(value) {
            field = value
            style.native = (value as? HTMLElement)?.style ?: (value as? SVGElement)?.style
            attributes.native = value
        }

    public fun hydrate(value: Element) {
        id = value.id
        content = value.innerHTML.takeUnless { it.isBlank() }
        this.element = value
    }

    public inline fun onElement(crossinline action: (Element) -> Unit) {
        element?.let(action) ?: elementToDo.add { action(it) }
    }

    public fun create(): Element {
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

    public actual fun click() {
        onElement { (it as HTMLElement).click() }
    }

    public actual fun focus() {
        onElement { (it as HTMLElement).focus() }
    }

    public actual fun blur() {
        onElement { (it as HTMLElement).blur() }
    }

    public actual fun screenRectangle(): Rect? {
        return element?.getBoundingClientRect()?.let {
            Rect(
                left = it.left,
                right = it.right,
                top = it.top,
                bottom = it.bottom,
            )
        }
    }

    public actual var xmlns: String? = null
    public actual var tag: String = "tag"
    public val attributesBack: Json = json()
    public actual val attributes: FutureElementAttributes = FutureElementAttributes(attributesBack)
    public val styleBack: Json = json()
    public actual val style: FutureElementStyle = FutureElementStyle(styleBack)
    public actual var desiredVerticalGravity: Align? = null
    public actual var desiredHorizontalGravity: Align? = null
    public val eventsBack: Json = json()
    public actual inline fun addEventListener(
        name: String,
        crossinline listener: (Event) -> Unit
    ) {
        element?.addEventListener(name, { it: Event -> listener(it) }) ?: run {
            @Suppress("UNCHECKED_CAST") val old = eventsBack["on$name"] as? (Event) -> Unit
            eventsBack["on$name"] = { it: Event -> old?.invoke(it); listener(it) }
        }
    }

    public actual inline fun replaceEventListener(
        name: String,
        crossinline listener: (Event) -> Unit
    ) {
        element?.let { it.asDynamic()["on$name"] = { it: Event -> listener(it) } } ?: run {
            eventsBack["on$name"] = { it: Event -> listener(it) }
        }
    }

    public val futureStyles: Json = json()
    public actual fun setStyleProperty(key: String, value: String?) {
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

    public val futureAttributes: Json = json()
    public actual fun setAttribute(key: String, value: String?) {
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


    public actual var classes: MutableSet<String> = ClassSet()
    public actual inline fun flushClasses() {}
    public actual var id: String? = null
        set(value) {
            field = value
            element?.id = value ?: Random.nextInt().toString()
        }
    public actual var content: String? = null
        set(value) {
            field = value
            value?.let {
                (element as? HTMLElement)?.innerText = value
            }
        }
    public actual var innerHtmlUnsafe: String? = null
        set(value) {
            field = value
            value?.let {
                (element as? HTMLElement)?.innerHTML = value
            }
        }
    private val lastChildren = ArrayList<FutureElement>()
    public actual val children: List<FutureElement>
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

    public actual fun appendChild(element: FutureElement) {
        assertSizeMatch()
        lastChildren.add(element)
        this.element?.let {
            it.appendChild(element.create())
        }
        assertSizeMatch()
    }

    public actual fun appendChild(index: Int, element: FutureElement) {
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

    public actual fun removeChild(index: Int) {
        assertSizeMatch()
        lastChildren.removeAt(index)
        element?.let {
            it.children.item(index)?.let { v -> it.removeChild(v) }
        }
        assertSizeMatch()
    }

    public actual fun clearChildren() {
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

    public inner class ClassSet : MutableSet<String> {
        public val map: HashSet<String> = HashSet<String>()
        public override fun add(element: String): Boolean = this@FutureElement.element?.addClass(element) ?: map.add(element)
        public override fun addAll(elements: Collection<String>): Boolean =
            this@FutureElement.element?.addClass(*elements.toTypedArray()) ?: map.addAll(elements)

        public override val size: Int get() = this@FutureElement.element?.classList?.length ?: map.size
        public override fun clear(): Unit = element?.let { it.className = "" } ?: map.clear()
        public override fun isEmpty(): Boolean = element?.className?.isBlank() ?: map.isEmpty()
        public override fun containsAll(elements: Collection<String>): Boolean = elements.all { contains(it) }
        public override fun contains(element: String): Boolean =
            this@FutureElement.element?.let { it.hasClass(element) } ?: map.contains(element)

        public override fun iterator(): MutableIterator<String> = this@FutureElement.element?.let {
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

        public override fun retainAll(elements: Collection<String>): Boolean = throw NotImplementedError()
        public override fun remove(element: String): Boolean =
            this@FutureElement.element?.removeClass(element) ?: map.remove(element)

        public override fun removeAll(elements: Collection<String>): Boolean =
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

public actual class FutureElementStyle(public var native: dynamic)
public actual class FutureElementAttributes(public var native: dynamic)

public fun Align?.logicalPosition(): ScrollLogicalPosition = when (this) {
    Align.Start -> ScrollLogicalPosition.START
    Align.Center -> ScrollLogicalPosition.CENTER
    Align.End -> ScrollLogicalPosition.END

    Align.Stretch -> ScrollLogicalPosition.START
    null -> ScrollLogicalPosition.NEAREST
}

public actual fun RView.nativeScrollIntoView(
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
public inline fun objectAssign(target: dynamic, source: dynamic): Any? = js("Object.assign(target, source)")
public actual fun RView.nativeSetDragData(data: DragData?) {
    native.onElement {
        if (data != null) {
            (it as HTMLElement).ondragstart = {
                it.stopPropagation()
                for((type, value) in data.typeToData) {
                    it.dataTransfer!!.setData(type, value)
                }
            }
        } else {
            (it as HTMLElement).ondragstart = null
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