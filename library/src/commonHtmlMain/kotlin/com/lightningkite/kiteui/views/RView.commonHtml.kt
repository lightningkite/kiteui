package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.RowOrCol

public actual abstract class RView public actual constructor(context: RContext) : RViewHelper(context) {
    public var native: FutureElement = FutureElement()

    public actual override var showOnPrint: Boolean = true
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
            (parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
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
            native.setStyleProperty("--spacing", value?.value?.toString())
        }

    override var paddingByEdge: Edges?
        get() = super.paddingByEdge
        set(value) {
            super.paddingByEdge = value
            native.style.paddingLeft = value?.left?.value?.toString() ?: "unset"
            native.style.paddingTop = value?.top?.value?.toString() ?: "unset"
            native.style.paddingRight = value?.right?.value?.toString() ?: "unset"
            native.style.paddingBottom = value?.bottom?.value?.toString() ?: "unset"
        }

    override var ignoreInteraction: Boolean
        get() = super.ignoreInteraction
        set(value) {
            super.ignoreInteraction = value
            if (value) native.classes.add("noInteraction")
            else native.classes.remove("noInteraction")
        }

    // drag 'n drop
    override var dragData: DragData?
        get() = super.dragData
        set(value) {
            super.dragData = value
            native.attributes.draggable = value != null
            nativeSetDragData(value)
        }
    override var dropTargetDelegate: DropTargetDelegate?
        get() = super.dropTargetDelegate
        set(value) {
            super.dropTargetDelegate = value
            nativeOnDrop(value)
        }


    public actual override fun scrollIntoView(
        horizontal: Align?,
        vertical: Align?,
        animate: Boolean
    ): Unit = nativeScrollIntoView(horizontal, vertical, animate)

    public actual override fun requestFocus() {
        native.setAttribute("autofocus", "true")
        afterTimeout(100) {
            native.focus()
        }
    }

    public actual override fun screenRectangle(): Rect? {
        return native.screenRectangle()
    }

    override fun leakDetect() {
        WeakReference(native).checkLeakAfterDelay(1000)
        native.actualElementForLeakTracking?.let {
            WeakReference(it).checkLeakAfterDelay(1000)
        }
    }

    protected var prevThemeClass: String? = null
    public actual override fun applyTheme(theme: ThemeAndBack) {
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

        native.setStyleProperty("--parentSpacing", parent?.mySpacingForChildren?.value?.toString() ?: "0px")
        native.flushClasses()
    }

    public actual override fun internalAddChild(index: Int, view: RView) {
        native.appendChild(index, view.native)
    }

    public actual override fun internalRemoveChild(index: Int) {
        native.removeChild(index)
    }

    public actual override fun internalClearChildren() {
        native.clearChildren()
    }

    init {
        this.working.addListener {
            //TODO: Make it take longer for 'working' semantic to apply
            if (working.value) native.classes.add("working")
            else native.classes.remove("working")
        }
    }

    public companion object {
        private var idCounter: Int = 0
    }
}

public typealias HtmlElementLike = FutureElement

public expect class FutureElementStyle
public expect class FutureElementAttributes

public expect class FutureElement {
    public constructor()

    public val actualElementForLeakTracking: Any?
    public var xmlns: String?
    public var tag: String
    public val attributes: FutureElementAttributes
    public val style: FutureElementStyle
    public var desiredVerticalGravity: Align?
    public var desiredHorizontalGravity: Align?
    public fun setAttribute(key: String, value: String?)
    public fun setStyleProperty(key: String, value: String?)
    public inline fun addEventListener(name: String, crossinline listener: (Event) -> Unit)
    public inline fun replaceEventListener(name: String, crossinline listener: (Event) -> Unit)
    public var classes: MutableSet<String>
    public inline fun flushClasses()
    public var id: String?
    public var content: String?
    public var innerHtmlUnsafe: String?
    public val children: List<FutureElement>
    public fun appendChild(element: FutureElement)
    public fun appendChild(index: Int, element: FutureElement)
    public fun removeChild(index: Int)
    public fun clearChildren()
    public fun click()
    public fun focus()
    public fun blur()
    public fun screenRectangle(): Rect?
}

public expect fun RView.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
)

public expect fun RView.nativeSetDragData(data: DragData?)
public expect fun RView.nativeOnDrop(listener: DropTargetDelegate?)