@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.write
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Deprecated("Renamed", ReplaceWith("ContainerElement"))
typealias ContainingView = ContainerElement

@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2")) typealias RecyclerView = Recycler2
@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2")) typealias ViewPager = Recycler2
@Deprecated("Renamed to Frame", ReplaceWith("Frame")) typealias Stack = Frame

@OptIn(ExperimentalContracts::class)
@ViewDsl
@Deprecated("Renamed to frame", ReplaceWith("frame(setup)"))
inline fun ElementWriter.stack(setup: Frame.() -> Unit = {}): Frame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Frame(context) , setup)
}