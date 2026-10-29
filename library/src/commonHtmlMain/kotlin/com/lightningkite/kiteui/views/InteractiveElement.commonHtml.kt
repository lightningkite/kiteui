package com.lightningkite.kiteui.views

public actual abstract class NativeInteractiveElement actual constructor(context: ElementContext) : NativeElement(context), InteractiveElement {
    actual override var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
            native.setAttribute("aria-disabled", if (value) null else "true")
        }
    public actual open var required: Boolean = false
        set(value) {
            field = value
            native.setAttribute("aria-required", if (value) "true" else null)
        }
}

public actual abstract class NativeInteractiveContainerElement actual constructor(context: ElementContext) : NativeContainerElement(context), InteractiveElement {
    actual override var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
            native.setAttribute("aria-disabled", if (value) null else "true")
        }

    public actual open var accessibleExpanded: Boolean? = null
        set(value) {
            field = value
            native.setAttribute("aria-expanded", value?.toString())
        }

    public actual open var accessibleOpensDialog: Boolean = false
        set(value) {
            field = value
            native.setAttribute("aria-haspopup", if (value) "dialog" else null)
        }
}