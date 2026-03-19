package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement


expect class IconView(context: ElementContext) : NativeElement {

    var source: Icon?
    var description: String?
}