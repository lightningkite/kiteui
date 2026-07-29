package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public actual class CircularProgress actual constructor(context: ElementContext) :
    NativeElement(context) {
    public actual var ratio: Float
        get() = TODO("Not yet implemented")
        set(value) {}
}