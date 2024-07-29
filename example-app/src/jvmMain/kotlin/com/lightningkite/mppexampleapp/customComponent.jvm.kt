package com.lightningkite.mppexampleapp

import ViewWriter

actual class CustomComponent {
    actual var src: String
        get() = TODO("Not yet implemented")
        set(value) {}
}

actual fun ViewWriter.customComponent(setup: CustomComponent.() -> Unit) {
}
