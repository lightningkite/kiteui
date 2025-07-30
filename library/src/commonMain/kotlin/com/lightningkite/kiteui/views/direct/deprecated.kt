@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.usesTouchscreen
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

typealias ContainingView = RView

@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2")) typealias RecyclerView = Recycler2
@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2")) typealias ViewPager = Recycler2
@Deprecated("Renamed to Frame", ReplaceWith("Frame")) typealias Stack = Frame

@OptIn(ExperimentalContracts::class)
@ViewDsl
@Deprecated("Renamed to frame", ReplaceWith("frame(setup)"))
inline fun ViewWriter.stack(setup: Frame.() -> Unit = {}): Frame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Frame(context) , setup)
}