package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement


public expect class ProgressBar(context: ElementContext) : NativeElement {
    public var ratio: Float
}