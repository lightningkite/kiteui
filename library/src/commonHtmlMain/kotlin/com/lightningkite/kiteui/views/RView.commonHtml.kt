package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.*

actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {
    var native = FutureElement()

    actual override var showOnPrint: Boolean = true
        set(value) {
            if(value)
                native.classes.remove("do-not-print")
            else
                native.classes.add("do-not-print")
        }

    protected actual override fun opacitySet(value: Double) {
        native.style.opacity = value.toString()
    }

    protected actual override fun existsSet(value: Boolean) {
        native.attributes.hidden = !value
    }

    protected actual override fun visibleSet(value: Boolean) {
        native.style.visibility = if(value) "visible" else "hidden"
    }

    protected actual override fun spacingSet(value: Dimension?) {
        native.setStyleProperty("--spacing", value?.value)
    }

    protected actual override fun ignoreInteractionSet(value: Boolean) {
        if(value) native.classes.add("noInteraction")
        else native.classes.remove("noInteraction")
    }

    protected actual override fun forcePaddingSet(value: Boolean?) {
        when(value) {
            true -> native.classes.add("padded")
            else -> native.classes.remove("padded")
        }
        when(value) {
            false -> native.classes.add("unpadded")
            else -> native.classes.remove("unpadded")
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

    actual override fun screenRectangle(): Rect? {
        return native.screenRectangle()
    }

    override fun leakDetect() {
        // Do nothing.  No access to manual GC exists in JS.
    }

    actual override fun applyElevation(dimension: Dimension) {
    }

    actual override fun applyPadding(dimension: Dimension?) {
        if(dimension != null) native.classes.add("padded")
        else native.classes.remove("padded")
        if (useNavSpacing) native.classes.add("useNavSpacing")
        else native.classes.remove("useNavSpacing")
    }

    actual override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        if (fullyApply) native.classes.add("transition")
        else native.classes.remove("transition")

        native.classes.removeAll { it.startsWith("t-") }
        native.classes.addAll(context.kiteUiCss.themeInteractive(theme))

        native.setStyleProperty("--parentSpacing", parentSpacing.value)
    }

    actual override fun applyForeground(theme: Theme) {
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
            if(working.value) native.classes.add("working")
            else native.classes.remove("working")
        }
        if(this.hasAlternateBackedStates()) native.classes.add("mightTransition")
    }

    companion object { private var idCounter: Int = 0 }
    private val randomId = idCounter++
    fun toStringWithoutParent(): String = "${this::class.simpleName}#${randomId}"
    override fun toString(): String = "${toStringWithoutParent()}, child of ${parent?.toStringWithoutParent()}"
}

typealias HtmlElementLike = FutureElement

expect class FutureElementStyle
expect class FutureElementAttributes

expect class FutureElement {
    constructor()
    var xmlns: String?
    var tag: String
    val attributes: FutureElementAttributes
    val style: FutureElementStyle
    var desiredVerticalGravity: Align?
    var desiredHorizontalGravity: Align?
    fun setAttribute(key: String, value: String?)
    fun setStyleProperty(key: String, value: String?)
    inline fun addEventListener(name: String, crossinline listener: (Event)->Unit)
    inline fun replaceEventListener(name: String, crossinline listener: (Event)->Unit)
    var classes: MutableSet<String>
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

interface DomValueMap<V> {
    operator fun get(key: String): V?
    operator fun set(key: String, value: V?)
    fun keysHashCode(): Int
    fun contentHashCode(): Int
    fun contentEquals(record: DomValueMap<V>): Boolean
}
