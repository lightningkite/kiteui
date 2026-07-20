package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public actual class Separator actual constructor(context: ElementContext) : NativeElement(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-separator")
    }
}