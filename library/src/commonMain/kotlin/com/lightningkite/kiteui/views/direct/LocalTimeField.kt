package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.*
import kotlinx.datetime.LocalTime


expect class LocalTimeField(context: ElementContext) : RViewWithAction {

    val content: MutableReactiveValue<LocalTime?>
    var range: ClosedRange<LocalTime>?

}