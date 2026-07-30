package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.sandbox
import com.lightningkite.kiteui.views.src


public actual class WebView actual constructor(context: ElementContext): NativeElement(context) {
    init {
        native.tag = "iframe"

    }
    public actual inline var url: String
        get() = native.attributes.src ?: ""
        set(value) {
            native.attributes.src = value
        }
    public actual var permitJs: Boolean = true
        set(value) {
            field = value
            // No sandbox attribute means an unrestricted iframe (scripts allowed); omitting
            // allow-scripts from the sandbox token list is what actually disables JS execution.
            // Written via setAttribute because HTMLIFrameElement.sandbox is a read-only
            // DOMTokenList in the DOM, so assigning it directly throws in strict mode.
            native.setAttribute(
                "sandbox",
                if (value) null else "allow-same-origin allow-forms allow-popups allow-modals"
            )
        }
    public actual inline var content: String
        get() = TODO()
        set(value) {
            TODO()
        }
}
