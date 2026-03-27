package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*
import kotlinx.datetime.LocalTime

expect class LocalTimeField(context: ElementContext) : NativeElementWithAction {
    val content: MutableReactiveValue<LocalTime?>
    var range: ClosedRange<LocalTime>?
}