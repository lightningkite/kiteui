@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.signal.invoke
import com.lightningkite.kiteui.usesTouchscreen
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

public typealias ContainingView = RView

@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2"))
public typealias RecyclerView = Recycler2
@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2"))
public typealias ViewPager = Recycler2
@Deprecated("Renamed to Frame", ReplaceWith("Frame"))
public typealias Stack = Frame

@OptIn(ExperimentalContracts::class)
@ViewDsl
@Deprecated("Renamed to frame", ReplaceWith("frame(setup)"))
public inline fun ViewWriter.stack(setup: Frame.() -> Unit = {}): Frame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Frame(context) , setup)
}