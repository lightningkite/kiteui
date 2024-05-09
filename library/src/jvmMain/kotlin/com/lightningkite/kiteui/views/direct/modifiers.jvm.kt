package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.ViewModifierDsl3
import ViewWriter

@ViewModifierDsl3
actual fun ViewWriter.onlyWhen(default: Boolean, condition: suspend ()->Boolean): ViewWrapper {
    beforeNextElementSetup {
        exists = true
        ::exists.invoke(condition)
    }
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.changingWeight(amount: suspend () -> Float): ViewWrapper {
    TODO("Not yet implemented")
}