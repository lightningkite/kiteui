package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.*

actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    var native = FutureElement()

    actual override var showOnPrint: Boolean = true
        set(value) {
            if (value)
                native.classes.remove("do-not-print")
            else
                native.classes.add("do-not-print")
        }

    override var opacity: Double
        get() = super.opacity
        set(value) {
            super.opacity = value
            native.style.opacity = value.toString()
        }

    override var shown: Boolean
        get() = super.shown
        set(value) {
            super.shown = value
            native.attributes.hidden = !value
        }

    override var visible: Boolean
        get() = super.visible
        set(value) {
            super.visible = value
            native.style.visibility = if (value) "visible" else "hidden"
        }

    override var gap: Dimension?
        get() = super.gap
        set(value) {
            super.gap = value
            native.setStyleProperty("--spacing", value?.value)
        }

    override var paddingByEdge: Edges?
        get() = super.paddingByEdge
        set(value) {
            super.paddingByEdge = value
            native.style.paddingLeft = value?.left?.value ?: "unset"
            native.style.paddingTop = value?.top?.value ?: "unset"
            native.style.paddingRight = value?.right?.value ?: "unset"
            native.style.paddingBottom = value?.bottom?.value ?: "unset"
        }

    override var ignoreInteraction: Boolean
        get() = super.ignoreInteraction
        set(value) {
            super.ignoreInteraction = value
            if (value) native.classes.add("noInteraction")
            else native.classes.remove("noInteraction")
        }


    actual override fun scrollIntoView(
        horizontal: Align?,
        vertical: Align?,
        animate: Boolean
    ) = nativeScrollIntoView(horizontal, vertical, animate)

    actual override fun requestFocus() {
        native.setAttribute("autofocus", "true")
        afterTimeout(100) {
            native.focus()
        }
    }

    actual override fun screenRectangle(): Rect? {
        return native.screenRectangle()
    }

    override fun leakDetect() {
        WeakReference(native).checkLeakAfterDelay(1000)
        native.actualElementForLeakTracking?.let {
            WeakReference(it).checkLeakAfterDelay(1000)
        }
    }

    private var prevThemeClass: String? = null
    actual override fun applyTheme(theme: ThemeAndBack) {
        if(theme.drawBackground) {
            native.classes.add("transition")
        } else {
            native.classes.remove("transition")
        }
        if(parent == null) println("Root element: $theme because ${themeChoice}")
        if (theme.padding) {
            native.classes.add("padded")
        } else {
            native.classes.remove("padded")
        }

        prevThemeClass?.let { native.classes.remove(it) }
        val newClass = context.kiteUiCss.themeInteractive(theme.theme)
        prevThemeClass = newClass
        native.classes.add(newClass)

        native.setStyleProperty("--parentSpacing", parentSpacing.value)
        native.flushClasses()
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        native.appendChild(index, view.native)
    }

    actual override fun internalRemoveChild(index: Int) {
        native.removeChild(index)
    }

    actual override fun internalClearChildren() {
        native.clearChildren()
    }

    init {
        this.working.addListener {
            //TODO: Make it take longer for 'working' semantic to apply
            if (working.value) native.classes.add("working")
            else native.classes.remove("working")
        }
    }

    companion object {
        private var idCounter: Int = 0
    }
}

typealias HtmlElementLike = FutureElement

expect class FutureElementStyle
expect class FutureElementAttributes

expect class FutureElement {
    constructor()

    val actualElementForLeakTracking: Any?
    var xmlns: String?
    var tag: String
    val attributes: FutureElementAttributes
    val style: FutureElementStyle
    var desiredVerticalGravity: Align?
    var desiredHorizontalGravity: Align?
    fun setAttribute(key: String, value: String?)
    fun setStyleProperty(key: String, value: String?)
    inline fun addEventListener(name: String, crossinline listener: (Event) -> Unit)
    inline fun replaceEventListener(name: String, crossinline listener: (Event) -> Unit)
    var classes: MutableSet<String>
    inline fun flushClasses()
    var id: String?
    var content: String?
    var innerHtmlUnsafe: String?
    val children: List<FutureElement>
    fun appendChild(element: FutureElement)
    fun appendChild(index: Int, element: FutureElement)
    fun removeChild(index: Int)
    fun clearChildren()
    fun click()
    fun focus()
    fun blur()
    fun screenRectangle(): Rect?
}

expect fun RView.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
)
