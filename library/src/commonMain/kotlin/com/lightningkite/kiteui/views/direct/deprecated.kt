@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactive
import com.lightningkite.kiteui.usesTouchscreen
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext

typealias ContainingView = RView

@Deprecated("Renamed to Recycler2") typealias RecyclerView = Recycler2
@Deprecated("Renamed to Recycler2") typealias ViewPager = Recycler2

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.recyclerView(setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return Recycler2(this, true).apply(setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.viewPager(setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return Recycler2(this, true).apply {

        placer = RecyclerViewPagingPlacer()
        snapToElements = Align.Center
        scrollSnapStop = true

        with(outerStack) {
            if(!Platform.usesTouchscreen) {
                gravity(Align.Start, Align.Center) - button {
                    icon(Icon.chevronLeft, "Previous")
                    onClick { centerIndex set centerIndex() - 1 }
                }
                gravity(Align.End, Align.Center) - button {
                    icon(Icon.chevronRight, "Next")
                    onClick { centerIndex set centerIndex() + 1 }
                }
            }
        }
    }.apply(setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.horizontalRecyclerView(setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return Recycler2(this, false).apply(setup)
}