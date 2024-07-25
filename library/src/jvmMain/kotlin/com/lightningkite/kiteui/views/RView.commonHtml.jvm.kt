package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Align

actual class FutureElementStyle(val underlyingMap: MutableMap<String, String>) {
    operator fun get(key: String): String? = underlyingMap.get(key)
    operator fun set(key: String, value: String?) {
        if (value == null) underlyingMap.remove(key)
        else underlyingMap.set(key, value)
    }
}

actual class FutureElementAttributes(val underlyingMap: MutableMap<String, String>) {
    operator fun get(key: String): String? = underlyingMap.get(key)
    operator fun set(key: String, value: String?) {
        if (value == null) underlyingMap.remove(key)
        else underlyingMap.set(key, value)
    }
}

actual class FutureElement actual constructor() {
    actual var xmlns: String? = null
    actual var tag: String = "tag"
    actual var classes: MutableSet<String> = HashSet()
    actual var id: String? = null
    actual var content: String? = null
    val childrenBack = ArrayList<FutureElement>()
    actual val children: List<FutureElement> = childrenBack
    actual fun appendChild(element: FutureElement) {
        childrenBack.add(element)
    }

    actual fun appendChild(index: Int, element: FutureElement) {
        childrenBack.add(index, element)
    }

    actual fun removeChild(index: Int) {
        childrenBack.removeAt(index)
    }

    actual fun clearChildren() {
        childrenBack.clear()
    }

    actual fun click() {}
    actual fun focus() {}
    actual fun blur() {}

    actual val attributes: FutureElementAttributes = FutureElementAttributes(HashMap())
    actual val style: FutureElementStyle = FutureElementStyle(HashMap())
    actual var desiredVerticalGravity: Align? = null
    actual var desiredHorizontalGravity: Align? = null
    actual fun setAttribute(key: String, value: String?) {
        if (value == null) attributes.underlyingMap.remove(key)
        else attributes.underlyingMap[key] = value
    }
    actual fun setStyleProperty(key: String, value: String?) {
        if (value == null) style.underlyingMap.remove(key)
        else style.underlyingMap[key] = value
    }

    actual inline fun addEventListener(name: String, listener: (Event) -> Unit) {}
    actual inline fun replaceEventListener(name: String, listener: (Event) -> Unit) {}

    fun render(out: Appendable) {
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

fun Appendable.appendSafe(html: String) {
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

actual fun RView.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
}