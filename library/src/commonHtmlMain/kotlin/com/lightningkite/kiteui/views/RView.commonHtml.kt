package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px

actual abstract class RView(context: RContext) : RViewHelper(context) {
    var native = FutureElement()
    var outerElement: FutureElement = native
    var styleElement: FutureElement = native
    var innerElement: FutureElement = native

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
        native.classes.removeAll { it.startsWith("spacingOf") }
        value?.let { value ->
            val cn = "spacingOf${value.value.replace(".", "_").filter { it.isLetterOrDigit() || it == '_' }}"
            context.dynamicCss.styleIfMissing(
                ".$cn.$cn.$cn.$cn.$cn.$cn.$cn.$cn > *, .$cn.$cn.$cn.$cn.$cn.$cn.$cn.$cn > .hidingContainer > *", mapOf(
                    "--parentSpacing" to value.value
                )
            )
            context.dynamicCss.styleIfMissing(
                ".$cn.$cn.$cn.$cn.$cn.$cn.$cn.$cn.mightTransition > *, .$cn.$cn.$cn.$cn.$cn.$cn.$cn.$cn.mightTransition > .hidingContainer > *", mapOf(
                    "--parentPadding" to value.value
                )
            )
            native.classes.add(cn)
        }
    }

    protected actual override fun ignoreInteractionSet(value: Boolean) {
        if(value)
            native.classes.add("noInteraction")
        else
            native.classes.remove("noInteraction")
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
    }

    actual override fun applyElevation(dimension: Dimension) {
    }

    actual override fun applyPadding(dimension: Dimension?) {
        if (useNavSpacing) native.classes.add("useNavSpacing")
        else native.classes.remove("useNavSpacing")
        if(dimension == null) native.classes.remove("mightTransition")
        else native.classes.add("mightTransition")
    }

    actual override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        if (fullyApply) native.classes.add("transition")
        else native.classes.remove("transition")

        native.classes.removeAll { it.startsWith("t-") }
        native.classes.add(context.kiteUiCss.themeInteractive(theme))
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
            if(working.value) native.classes.add("loading")
            else native.classes.remove("loading")
        }
    }
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
    fun setAttribute(key: String, value: String?)
    fun setStyleProperty(key: String, value: String?)
    inline fun addEventListener(name: String, crossinline listener: (Event)->Unit)
    var classes: MutableSet<String>
    var id: String?
    var content: String?
    val children: List<FutureElement>
    fun appendChild(element: FutureElement)
    fun appendChild(index: Int, element: FutureElement)
    fun removeChild(index: Int)
    fun clearChildren()
    fun click()
    fun focus()
    fun blur()
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
