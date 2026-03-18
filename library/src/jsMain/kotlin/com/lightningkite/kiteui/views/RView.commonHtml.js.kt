package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DropTargetDelegate
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.ssr.HydrationContext
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

    /**
     * Hydrate this FutureElement with an existing DOM element.
     * Attaches event listeners and syncs state without recreating the element.
     * @return true if hydration succeeded, false if tag mismatch occurred
     *
     * Updated by Claude to record hydration statistics.
     */
    fun hydrate(existingElement: Element): Boolean {
        // Validate tag match
        if (tag.lowercase() != existingElement.tagName.lowercase()) {
            console.warn("Hydration mismatch: expected <$tag>, found <${existingElement.tagName}>")
            console.warn("  Parent: ${existingElement.parentElement?.tagName}, classes: ${existingElement.parentElement?.className}")
            HydrationContext.recordMismatch()
            return false
        }

        // Capture pending classes before linking element (ClassSet switches to DOM after element is set)
        val pendingClasses = classes.toList()

        // Link to existing element
        this.element = existingElement
        id = existingElement.id.takeIf { it.isNotBlank() }

        // Apply pending event listeners
        forEach(eventsBack) { name, handler ->
            existingElement.asDynamic()[name] = handler
        }

        // Apply pending styles (sync differences)
        (existingElement as? HTMLElement)?.style?.let { style ->
            forEach(futureStyles) { k, v -> style.setProperty(k, v) }
        } ?: (existingElement as? SVGElement)?.style?.let { style ->
            forEach(futureStyles) { k, v -> style.setProperty(k, v) }
        }

        // Apply pending attributes
        forEach(futureAttributes) { k, v -> existingElement.setAttribute(k, v) }

        // Sync classes - add any missing, but don't remove SSR classes
        pendingClasses.forEach { cls ->
            if (!existingElement.classList.contains(cls)) {
                existingElement.classList.add(cls)
            }
        }

        // Execute pending operations
        elementToDo.forEach { it(existingElement) }
        elementToDo.clear()

        // Record successful hydration - by Claude
        HydrationContext.recordHydrated()

        // Debug visualization: add subtle green outline to hydrated elements - by Claude
        if (debugMode) {
            (existingElement as? HTMLElement)?.style?.outline = "1px solid rgba(0, 200, 0, 0.3)"
        }

        return true
    }

    /**
     * Recursively hydrate this element and all children.
     * @return true if hydration succeeded, false if tag mismatch occurred
     *
     * Updated by Claude to record hydration statistics and handle mismatches gracefully.
     */
    fun hydrateRecursive(existingElement: Element): Boolean {
        if (!hydrate(existingElement)) return false

        // Hydrate children by position
        val existingChildren = existingElement.children
        lastChildren.forEachIndexed { index, childFuture ->
            val existingChild = existingChildren.item(index)
            if (existingChild != null) {
                if (!childFuture.hydrateRecursive(existingChild)) {
                    // Child hydration failed - replace SSR element with fresh JS element
                    // This ensures the JS FutureElement is properly linked to DOM
                    // by Claude
                    val newElement = childFuture.create()
                    existingChild.parentElement?.replaceChild(newElement, existingChild)
                    HydrationContext.recordCreated()
                    // Debug visualization: add red outline to newly created elements - by Claude
                    if (debugMode) {
                        (newElement as? HTMLElement)?.style?.outline = "1px solid rgba(255, 0, 0, 0.5)"
                    }
                }
            } else {
                // More RView children than DOM children - append new ones
                if (debugMode) {
                    console.warn("Hydration: RView has more children than DOM at index $index")
                }
                HydrationContext.recordCreated()
                val newElement = childFuture.create()
                existingElement.appendChild(newElement)
                // Debug visualization: add orange outline for newly appended elements - by Claude
                if (debugMode) {
                    (newElement as? HTMLElement)?.style?.outline = "1px solid rgba(255, 165, 0, 0.5)"
                }
            }
        }

        // Remove extra DOM children that don't have corresponding RView children
        // This prevents duplicate content from SSR elements that don't exist in JS render
        // Optimized to calculate count upfront and avoid repeated length checks - by Claude
        val extraCount = existingElement.children.length - lastChildren.size
        if (extraCount > 0) {
            if (debugMode) {
                console.warn("Hydration: Removing $extraCount extra DOM children from <${existingElement.tagName}>")
            }
            repeat(extraCount) {
                existingElement.lastElementChild?.let { extra ->
                    existingElement.removeChild(extra)
                }
            }
        }

        return true
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
    actual fun parentRectangle(): Rect? {
        return element?.let {
            it as HTMLElement
            Rect(left = it.offsetLeft.toDouble(), top = it.offsetTop.toDouble(), right = it.offsetLeft.toDouble() + it.scrollWidth.toDouble(), bottom = it.offsetTop.toDouble() + it.scrollHeight.toDouble())
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
                            event.dataTransfer!!.setDragImage(
                                it,
                                x = when (shadow.xAlign) {
                                    Align.Start -> 0
                                    Align.Center, Align.Stretch -> (it.getBoundingClientRect().width / 2).roundToInt()
                                    Align.End -> it.getBoundingClientRect().width.roundToInt()
                                } + (shadow.xOffset?.px?.roundToInt() ?: 0),
                                y = when (shadow.yAlign) {
                                    Align.Start -> 0
                                    Align.Center, Align.Stretch -> (it.getBoundingClientRect().height / 2).roundToInt()
                                    Align.End -> it.getBoundingClientRect().height.roundToInt()
                                } + (shadow.yOffset?.px?.roundToInt() ?: 0)
                            )
                        }
                    }
                }
            }
        } else {
            (element as HTMLElement).ondragstart = null
            element.ondragend = null
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
            it.ondragenter = null
            it.ondragleave = null
            it.ondragexit = null
            it.ondrop = null
        }
    }
}