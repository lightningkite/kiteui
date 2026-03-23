@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate

actual abstract class NativeElement actual constructor(context: ElementContext) : NativeElementCommonCode(context) {
    var native = FutureElement().also { it.classes.add("kui") }

    actual override var opacity: Double = 1.0
        set(value) {
            field = value
            native.style.opacity = value.toString()
        }

    actual override var shown: Boolean = true
        set(value) {
            field = value
            native.attributes.hidden = !value
            // TODO (parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
        }

    actual override var visible: Boolean = true
        set(value) {
            field = value
            native.style.visibility = if (value) "visible" else "hidden"
        }

    actual override var ignoreInteraction: Boolean = false
        set(value) {
            field = value
            if (value) native.classes.add("noInteraction")
            else native.classes.remove("noInteraction")
        }

    override var debugName: String?
        get() = super.debugName
        set(value) {
            super.debugName = value
            native.setAttribute("data-debug-name", value ?: "")
        }

    // drag 'n drop
    actual override var dragData: DragData? = null
        set(value) {
            field = value
            native.attributes.draggable = value != null

            if (value != null) native.classes += "draggable"
            else native.classes -= "draggable"

            nativeSetDragData(value)
        }

    actual override var dropTargetDelegate: DropTargetDelegate? = null
        set(value) {
            field = value
            nativeOnDrop(value)
        }

    actual override fun refreshPadding() {
        if (paddingByEdge != null || safeAreaPadding != null) {
            val value = appliedPadding
            native.style.paddingLeft = value.left.value.toString()
            native.style.paddingTop = value.top.value.toString()
            native.style.paddingRight = value.right.value.toString()
            native.style.paddingBottom = value.bottom.value.toString()
        } else {
            native.style.paddingLeft = null
            native.style.paddingTop = null
            native.style.paddingRight = null
            native.style.paddingBottom = null
        }
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

    actual fun screenRectangle(): Rect? {
        return native.screenRectangle()
    }

    actual fun parentRectangle(): Rect? {
        return native.parentRectangle()
    }

    override fun leakDetect() {
        WeakReference(native).checkLeakAfterDelay(1000)
        native.actualElementForLeakTracking?.let {
            WeakReference(it).checkLeakAfterDelay(1000)
        }
    }

    protected var prevThemeClass: String? = null
    actual override fun nativeApplyTheme(theme: ThemeAndBack) {
        if (theme.drawBackground) {
            native.classes.add("transition")
        } else {
            native.classes.remove("transition")
        }
        if (theme.padding) {
            native.classes.add("padded")
        } else {
            native.classes.remove("padded")
        }

        prevThemeClass?.let { native.classes.remove(it) }
        val newClass = context.kiteUiCss.themeInteractive(theme.theme)
        prevThemeClass = newClass
        native.classes.add(newClass)

        native.setStyleProperty("--parentSpacing", parent?.spacingForChildCornerRadii?.value?.toString() ?: "0px")
        native.flushClasses()
    }

    init {
        foregroundProcesses.addListener {   // auto-released when view is removed
            if (!foregroundProcesses.state.success) native.classes.add("working")
            else native.classes.remove("working")
        }
    }

    actual override var showOnPrint: Boolean = true
        set(value) {
            if (value)
                native.classes.remove("do-not-print")
            else
                native.classes.add("do-not-print")
        }
}

val Element.native: FutureElement get() = underlyingNativeElement.native

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
    fun parentRectangle(): Rect?
}

typealias HtmlElementLike = FutureElement

expect fun NativeElement.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
)

expect fun NativeElement.nativeSetDragData(data: DragData?)
expect fun NativeElement.nativeOnDrop(listener: DropTargetDelegate?)