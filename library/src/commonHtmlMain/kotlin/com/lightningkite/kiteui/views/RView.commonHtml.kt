package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.dom.Event
import com.lightningkite.kiteui.dom.HTMLElement
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme

actual abstract class RView(context: RContext) : RViewHelper(context) {
    var native: HtmlElementLike = FutureElement()
    lateinit var element: HTMLElement

    protected actual override fun opacitySet(value: Double) {
        native.style.opacity = value.toString()
    }

    protected actual override fun existsSet(value: Boolean) {
        native.attributes["hidden"] = "true"
    }

    protected actual override fun visibleSet(value: Boolean) {
        native.style.visibility = if(value) "visible" else "hidden"
    }

    protected actual override fun spacingSet(value: Dimension?) {
        native.style.record["--spacing"] = value?.value
        native.classes.removeAll { it.startsWith("spacingOf") }
        value?.let { value ->
            val cn = "spacingOf${value.value.replace(".", "_").filter { it.isLetterOrDigit() || it == '_' }}"
            DynamicCss.styleIfMissing(
                ".$cn.$cn.$cn.$cn.$cn.$cn.$cn.$cn > *, .$cn.$cn.$cn.$cn.$cn.$cn.$cn.$cn > .hidingContainer > *", mapOf(
                    "--parentSpacing" to value.value
                )
            )
            native.classes.add("spacingOf")
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
        native.attributes["autofocus"] = "true"
    }

    actual override fun applyElevation(dimension: Dimension) {
    }

    actual override fun applyPadding(dimension: Dimension) {
    }

    actual override fun applyBackground(theme: Theme, fullyApply: Boolean) {

        if (fullyApply) native.classes.add("transition")
        else native.classes.remove("transition")
//        if (mightTransition) virtualClasses.add("mightTransition")
//        else virtualClasses.remove("mightTransition")

        native.classes.removeAll { it.startsWith("theme-") }
        native.classes.add(KiteUiCss.themeInteractive(theme))
    }

    actual override fun applyForeground(theme: Theme) {
    }

    actual override fun internalAddChild(index: Int, view: RView) {
        native.children.add(index, view.native)
    }

    actual override fun internalRemoveChild(index: Int) {
        native.children.removeAt(index)
    }

    actual override fun internalClearChildren() {
        native.children.clear()
    }
}

interface HtmlElementLike {
    var tag: String
    val attributes: Record<String>
    val style: CSSStyleDeclaration
    val events: Record<(Event)->Unit>
    val classes: MutableSet<String>
    var id: String?
    var content: String?
    val children: MutableList<HtmlElementLike>

    fun render(out: Appendable) {
        out.append('<')
        out.append(tag)
        attributes.forEach { key, value ->
            out.append(' ')
            out.append(key)
            out.append("='")
            out.append(value)
            out.append('\'')
        }
        out.append(" class='")
        classes.joinToString(" ")
        out.append("' style='")
        style.record.forEach { key, value ->
            out.append(key)
            out.append(':')
            out.append(value.replace('\'', '"'))
            out.append(';')
        }
        out.append("'")
        if(children.isNotEmpty()) {
            out.append('>')
            children.forEach { it.render(out) }
            out.append("</")
            out.append(tag)
            out.append('>')
        } else if(content != null) {
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

class FutureElement: HtmlElementLike {
    override var tag: String = "span"
    override val attributes: Record<String> = Record()
    override val style: CSSStyleDeclaration = CSSStyleDeclaration()
    override val events: Record<(Event) -> Unit> = Record()
    override var classes: MutableSet<String> = HashSet()
    override var id: String? = null
    override var content: String? = null
    override val children: MutableList<HtmlElementLike> = ArrayList()

    val selfHash get() = tag.hashCode() +
            attributes.contentHashCode() +
            style.record.contentHashCode() +
            events.contentHashCode() +
            classes.hashCode() +
            id.hashCode() +
            content.hashCode()
}

expect fun RView.create(): HtmlElementLike
expect fun RView.hydrate(existing: HTMLElement): HtmlElementLike
expect fun Appendable.appendSafe(html: String)
expect fun RView.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
)

expect class Record<V>() {
    operator fun get(key: String): V?
    operator fun set(key: String, value: V?)
    fun forEach(action: (String, V)->Unit)
    fun contentHashCode(): Int
    fun contentEquals(record: Record<V>): Boolean
}

expect class HTMLElement