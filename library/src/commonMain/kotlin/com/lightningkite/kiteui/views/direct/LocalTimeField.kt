package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*
import kotlinx.datetime.LocalTime

public expect class LocalTimeField(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<LocalTime?>
    public var range: ClosedRange<LocalTime>?
}