package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.ReactiveContext
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.hidden

internal actual fun RView.nativeAnimateHideBinding(
    default: Boolean,
    condition: ReactiveContext<*>.() -> Boolean
) {
    native.attributes.hidden = !default
    reactiveScope {
        native.attributes.hidden = !condition()
    }
}