package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement


expect class CircularProgress(context: ElementContext) : NativeElement {
    var ratio: Float
}