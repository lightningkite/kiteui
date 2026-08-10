package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public expect class IconView(context: ElementContext) : NativeElement {
    public var source: Icon?
    public var description: String?
}