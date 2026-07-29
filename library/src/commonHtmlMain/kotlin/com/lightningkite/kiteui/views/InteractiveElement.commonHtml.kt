package com.lightningkite.kiteui.views

public actual abstract class NativeInteractiveElement actual constructor(context: ElementContext) : NativeElement(context), InteractiveElement {
    actual override var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
            native.setAttribute("aria-disabled", if (value) null else "true")
        }
}

public actual abstract class NativeInteractiveContainerElement actual constructor(context: ElementContext) : NativeContainerElement(context), InteractiveElement {
    actual override var enabled: Boolean
        get() = native.attributes.disabled != true
        set(value) {
            native.attributes.disabled = !value
            native.setAttribute("aria-disabled", if (value) null else "true")
        }
}