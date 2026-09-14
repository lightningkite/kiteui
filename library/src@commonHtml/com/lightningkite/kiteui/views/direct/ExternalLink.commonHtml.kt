package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.utils.safeLinkUrlOrNull
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.rel
import kotlinx.coroutines.launch
import com.lightningkite.kiteui.views.AiDriver


public actual class ExternalLink actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions: AiDriver.Actions get() = super.driverActions + externalLinkDriverActions()
    init {
        themeChoice += ClickableSemantic
        native.tag = "a"
        native.classes.add("kiteui-stack")
        native.classes.add("clickable")
    }
    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    // Not inline: the setter calls a validator, and an unsafe target must never reach href.
    // This is the sink where a javascript: or data: URL would actually execute, so it is
    // checked here as well as at the sources that build links from untrusted content.
    //
    // setAttribute rather than `attributes.href = ...`: the generated accessor assigns the DOM
    // *property*, and `href` is a non-nullable USVString in the IDL, so assigning null stringified
    // it to href="null" and a rejected link navigated to /null - live, and to a real page on any
    // site with a catch-all route. setAttribute(key, null) removes the attribute outright on both
    // js and jvmSsr, which is the only thing "no target" can safely mean.
    public actual var to: String?
        get() = native.attributes.href
        set(value) {
            native.setAttribute("href", safeLinkUrlOrNull(value))
        }

    public actual var newTab: Boolean
        get() = native.attributes.target == "_blank"
        set(value) {
            native.setAttribute("target", if (value) "_blank" else "_self")
            // rel for SEO and security on new-tab links. Same null-stringification trap as href
            // above: this used to emit rel="null" on every same-tab link.
            native.setAttribute("rel", if (value) "noopener noreferrer" else null)
        }

    private var eventListenerAdded = false
    private fun registerEventListener() {
        if (eventListenerAdded) return
        native.addEventListener("click") {
            action?.startAction(this)
            if (to != null) onNavigateAction?.startAction(this)
        }
        eventListenerAdded = true
    }

    override fun nativeSetAction(action: Action?) {
        registerEventListener()
        native.setAttribute("aria-label", accessibleLabel ?: action?.title)
    }
    override fun nativeSetSecondaryAction(action: Action?) { registerEventListener() }
}
