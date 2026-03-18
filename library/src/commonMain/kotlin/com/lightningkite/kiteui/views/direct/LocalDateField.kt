package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.*
import kotlinx.datetime.LocalDate


expect class LocalDateField(context: ElementContext) : RViewWithAction {
    val content: MutableReactiveValue<LocalDate?>
    var range: ClosedRange<LocalDate>?
}