package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.contracts.*
import kotlin.jvm.JvmInline


public expect class ToggleButton(context: RContext) : RView {

    public var enabled: Boolean
    public val checked: MutableReactiveValue<Boolean>
}