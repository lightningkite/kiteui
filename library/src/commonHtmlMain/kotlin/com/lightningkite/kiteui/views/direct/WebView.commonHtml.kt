package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.src


actual class WebView actual constructor(context: ElementContext): NativeElement(context) {
    init {
        native.tag = "iframe"

    }
    actual inline var url: String
        get() = native.attributes.src ?: ""
        set(value) {
            native.attributes.src = value
        }
    actual var permitJs: Boolean = true
    actual inline var content: String
        get() = TODO()
        set(value) {
            TODO()
        }
}
