package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*

private var autoCompleteDatalistIdCounter = 0

public actual class AutoCompleteTextField actual constructor(context: ElementContext) : NativeElementWithAction(context) {
    override val driverValue: String? get() = autoCompleteDriverValue()
    override val driverActions: Map<String, suspend (List<String>) -> String> get() = super.driverActions + autoCompleteDriverActions()
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    public actual val content: MutableReactiveValue<String> = native.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    public actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.applyKeyboardHints(value)
        }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }
    public actual var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    internal var align: Align = Align.Start
        set(value) {
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    internal var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }

    // The <datalist> backing this field's suggestions. It is wired to the <input> purely by the
    // list/id attribute pair (per the HTML datalist spec, it does not need to be a DOM sibling of
    // the input it decorates - just present anywhere in the same document). It cannot be a *child*
    // of `native` itself: `native` is an `<input>`, a void element, and FutureElement.render emits
    // any element with children as `<input>...</input>`, which is invalid markup no browser HTML
    // parser will structure the way we intend.
    //
    // Instead it is appended alongside `native` under the parent container, using the same
    // `parent?.native` escape hatch modifiers.commonHtml.kt already relies on (see
    // `childHasWeight`) for a leaf element to reach into its container's real DOM node.
    //
    // Created lazily, on the first `suggestions` write, rather than eagerly in onStartup(): `parent`
    // is null when this field is written as a page's sole root element with no wrapping container
    // (the root ViewWriter's willAddChild is a deliberate no-op - see root.kt/rootSetupIos.kt - so
    // it never assigns a parent). That is a real, legal way to write this element, so it must not
    // require a parent for plain text-field use. It only becomes a problem once `suggestions` is
    // actually used, at which point failing loudly beats the alternative: silently discarding the
    // list, which is exactly the bug this file used to have.
    private var datalist: FutureElement? = null

    private fun datalistAttachedToParent(): FutureElement {
        datalist?.let { return it }
        val parentNative = parent?.native
            ?: throw IllegalStateException(
                "AutoCompleteTextField.suggestions needs a parent container to host its <datalist> " +
                        "sibling (an <input> cannot have children - see FutureElement.render's void-element " +
                        "handling). This field has no parent, which happens when it is written as a page's " +
                        "sole root element with nothing wrapping it. Wrap it in a container, e.g. " +
                        "`col { autoCompleteTextField { ... } }`."
            )
        val id = "kiteui-autocomplete-${autoCompleteDatalistIdCounter++}"
        val d = FutureElement().apply {
            tag = "datalist"
            this.id = id
        }
        native.setAttribute("list", id)
        parentNative.appendChild(d)
        datalist = d
        return d
    }

    @OptIn(OverrideOnly::class)
    override fun onShutdown() {
        datalist?.let { d ->
            parent?.native?.let { p ->
                val index = p.children.indexOf(d)
                if (index >= 0) p.removeChild(index)
            }
        }
        super.onShutdown()
    }

    public actual var suggestions: List<String> = listOf()
        set(value) {
            field = value
            val d = datalistAttachedToParent()
            d.clearChildren()
            for (suggestion in value) {
                d.appendChild(FutureElement().apply {
                    tag = "option"
                    // `setAttribute` goes through FutureElement's HTML-escaping on SSR render
                    // (`appendSafe`) and the real DOM `setAttribute` in the browser, so a suggestion
                    // containing quotes or angle brackets cannot break out of the attribute or
                    // inject markup.
                    setAttribute("value", suggestion)
                })
            }
        }
}