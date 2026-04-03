package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.rel
import kotlinx.coroutines.launch


actual class ExternalLink actual constructor(context: ElementContext) : NativeContainerElementWithSecondaryAction(context) {
    override val driverActions get() = super.driverActions + externalLinkDriverActions()
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

    actual inline var to: String?
        get() = native.attributes.href
        set(value) {
            native.attributes.href = value
        }

    actual inline var newTab: Boolean
        get() = native.attributes.target == "_blank"
        set(value) {
            native.attributes.target = if (value) "_blank" else "_self"
            // by Claude - set rel for SEO and security on new-tab links
            native.attributes.rel = if (value) "noopener noreferrer" else null
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

    override fun nativeSetAction(action: Action?) { registerEventListener() }
    override fun nativeSetSecondaryAction(action: Action?) { registerEventListener() }
}
