package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*
import kotlinx.datetime.LocalDate

public expect class LocalDateField(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<LocalDate?>
    public var range: ClosedRange<LocalDate>?
}