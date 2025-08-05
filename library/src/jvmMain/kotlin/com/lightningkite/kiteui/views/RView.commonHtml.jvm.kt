package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.Rect

@InternalKiteUi
public actual class FutureElementStyle(public val underlyingMap: MutableMap<String, String>) {
    public operator fun get(key: String): String? = underlyingMap.get(key)
    public operator fun set(key: String, value: String?) {
        if (value == null) underlyingMap.remove(key)
        else underlyingMap.set(key, value)
    }
}

@InternalKiteUi
public actual class FutureElementAttributes(public val underlyingMap: MutableMap<String, String>) {
    public operator fun get(key: String): String? = underlyingMap.get(key)
    public operator fun set(key: String, value: String?) {
        if (value == null) underlyingMap.remove(key)
        else underlyingMap.set(key, value)
    }
}

@InternalKiteUi
public actual class FutureElement public actual constructor() {
    @InternalKiteUi
    public actual val actualElementForLeakTracking: Any? get() = null
    @InternalKiteUi
    public actual var xmlns: String? = null
    @InternalKiteUi
    public actual var tag: String = "tag"
    @InternalKiteUi
    public actual var classes: MutableSet<String> = HashSet()
    @InternalKiteUi
    public actual var id: String? = null
    @InternalKiteUi
    public actual var content: String? = null
    @InternalKiteUi
    public actual var innerHtmlUnsafe: String? = null
    public val childrenBack: ArrayList<FutureElement> = ArrayList<FutureElement>()
    @InternalKiteUi
    public actual val children: List<FutureElement> = childrenBack
    @InternalKiteUi
    public actual fun appendChild(element: FutureElement) {
        childrenBack.add(element)
    }

    @InternalKiteUi
    public actual fun appendChild(index: Int, element: FutureElement) {
        childrenBack.add(index, element)
    }

    @InternalKiteUi
    public actual fun removeChild(index: Int) {
        childrenBack.removeAt(index)
    }

    @InternalKiteUi
    public actual fun clearChildren() {
        childrenBack.clear()
    }

    public actual fun click() {}
    public actual fun focus() {}
    public actual fun blur() {}
    public actual fun screenRectangle(): Rect? = null

    public actual inline fun flushClasses() {}

    @InternalKiteUi
    public actual val attributes: FutureElementAttributes = FutureElementAttributes(HashMap())
    @InternalKiteUi
    public actual val style: FutureElementStyle = FutureElementStyle(HashMap())
    @InternalKiteUi
    public actual var desiredVerticalGravity: Align? = null
    @InternalKiteUi
    public actual var desiredHorizontalGravity: Align? = null
    @InternalKiteUi
    public actual fun setAttribute(key: String, value: String?) {
        if (value == null) attributes.underlyingMap.remove(key)
        else attributes.underlyingMap[key] = value
    }
    @InternalKiteUi
    public actual fun setStyleProperty(key: String, value: String?) {
        if (value == null) style.underlyingMap.remove(key)
        else style.underlyingMap[key] = value
    }

    @InternalKiteUi
    public actual inline fun addEventListener(name: String, listener: (Event) -> Unit) {}
    @InternalKiteUi
    public actual inline fun replaceEventListener(name: String, listener: (Event) -> Unit) {}

    public fun render(out: Appendable) {
        out.append('<')
        out.append(tag)
        attributes.underlyingMap.forEach { (key, value) ->
            out.append(' ')
            out.append(key)
            out.append("='")
            out.append(value)
            out.append('\'')
        }
        xmlns?.let {
            out.append(" xmlns='$it'")
        }
        id?.let {
            out.append(" id='$it'")
        }
        out.append(" class='")
        classes.joinToString(" ")
        out.append("' style='")
        style.underlyingMap.forEach { (key, value) ->
            out.append(key)
            out.append(':')
            out.appendSafe(value)
            out.append(';')
        }
        out.append("'")
        if (children.isNotEmpty()) {
            out.append('>')
            children.forEach { it.render(out) }
            out.append("</")
            out.append(tag)
            out.append('>')
        } else if (content != null) {
            out.append('>')
            out.appendSafe(content ?: "")
            out.append("</")
            out.append(tag)
            out.append('>')
        } else {
            out.append("/>")
        }
    }
}

@InternalKiteUi
public fun Appendable.appendSafe(html: String) {
    for(char in html) {
        when(char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#x27;")
            else -> append(char)
        }
    }
}
@InternalKiteUi
public actual fun RView.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {

}

@InternalKiteUi
public actual fun RView.nativeSetDragData(data: DragData?) {}
@InternalKiteUi
public actual fun RView.nativeOnDrop(listener: DropTargetDelegate?) {}