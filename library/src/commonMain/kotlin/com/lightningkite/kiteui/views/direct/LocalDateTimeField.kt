package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*
import kotlinx.datetime.LocalDateTime


expect class LocalDateTimeField(context: ElementContext) : NativeElementWithAction {
    val content: MutableReactiveValue<LocalDateTime?>
    var range: ClosedRange<LocalDateTime>?
}