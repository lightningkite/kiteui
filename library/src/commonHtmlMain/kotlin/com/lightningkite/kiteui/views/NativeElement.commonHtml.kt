@file:OptIn(InternalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.DropTargetDelegate

private var labelForIdCounter = 0

public actual abstract class NativeElement actual constructor(context: ElementContext) : NativeElementCommonCode(context) {
    public var native = FutureElement().also { it.classes.add("kui") }

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

    // --- ACCESSIBILITY ---

    override var accessibleLabel: String?
        get() = super.accessibleLabel
        set(value) {
            super.accessibleLabel = value
            native.setAttribute("aria-label", value)
        }

    override var accessibleLiveRegion: LiveRegionMode
        get() = super.accessibleLiveRegion
        set(value) {
            super.accessibleLiveRegion = value
            when (value) {
                LiveRegionMode.None -> native.setAttribute("aria-live", null)
                LiveRegionMode.Polite -> native.setAttribute("aria-live", "polite")
                LiveRegionMode.Assertive -> native.setAttribute("aria-live", "assertive")
            }
        }

    override var labelFor: Element?
        get() = super.labelFor
        set(value) {
            val previous = super.labelFor
            super.labelFor = value
            if (value != null) {
                val targetNative = value.underlyingNativeElement.native
                if (targetNative.id == null) {
                    targetNative.id = "kiteui-a11y-${labelForIdCounter++}"
                }
                if (native.tag == "span" || native.tag == "p") {
                    native.tag = "label"
                    native.setAttribute("for", targetNative.id!!)
                } else {
                    targetNative.setAttribute("aria-labelledby", targetNative.id!!)
                }
            } else {
                if(native.tag == "label") native.setAttribute("for", null)
                else previous?.underlyingNativeElement?.native?.setAttribute("aria-labelledby", null)
            }
        }

    override var describedBy: Element?
        get() = super.describedBy
        set(value) {
            super.describedBy = value
            if (value != null) {
                val descNative = value.underlyingNativeElement.native
                if (descNative.id == null) {
                    descNative.id = "kiteui-a11y-${labelForIdCounter++}"
                }
                native.setAttribute("aria-describedby", descNative.id)
            } else {
                native.setAttribute("aria-describedby", null)
            }
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

    public actual fun screenRectangle(): Rect? {
        return native.screenRectangle()
    }

    public actual fun parentRectangle(): Rect? {
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

// TODO: transform this to point to the _STYLE PARTICIPATING_ element, not necessarily the direct element
// That's what we're using it for in every case it's used...
public val Element.native: FutureElement get() = underlyingNativeElement.native

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
    public fun parentRectangle(): Rect?
}

public typealias HtmlElementLike = FutureElement

public expect fun NativeElement.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
)

public expect fun NativeElement.nativeSetDragData(data: DragData?)
public expect fun NativeElement.nativeOnDrop(listener: DropTargetDelegate?)
