package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.views.ElementContext

import com.lightningkite.kiteui.views.RView


expect class IconView(context: ElementContext) : RView {

    var source: Icon?
    var description: String?
}