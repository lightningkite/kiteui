@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewModifierDsl3
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.context.ReactiveContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import com.lightningkite.kiteui.views.l2.label as l2Label

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


@ViewModifierDsl3
@Deprecated("Renamed for consistency", ReplaceWith("dynamicWeight(amount)"))
fun ElementWriter.CanAddWeight.changingWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddShownWhen = dynamicWeight(amount)

@ViewModifierDsl3
@Deprecated("Renamed for consistency", ReplaceWith("dynamicSizeConstraints(constraints)"))
fun ElementWriter.CanAddSizing.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme = dynamicSizeConstraints(constraints)

@Deprecated("Use VideoView instead", ReplaceWith("VideoView"), level = DeprecationLevel.ERROR) typealias Video = VideoView

@Deprecated("Import has moved", ReplaceWith("label(label, content)", "com.lightningkite.kiteui.views.l2.label"))
inline fun ElementWriter.label(label: String, content: LinearLayoutElement.() -> Unit) = l2Label(label, content)