package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*
import kotlinx.datetime.LocalDateTime


public expect class LocalDateTimeField(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<LocalDateTime?>
    public var range: ClosedRange<LocalDateTime>?
}