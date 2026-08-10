@file:Suppress("DEPRECATION")

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewModifierDsl3
import com.lightningkite.kiteui.views.l2.LabeledView
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.context.ReactiveContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import com.lightningkite.kiteui.views.l2.label as l2Label

@Deprecated("Renamed", ReplaceWith("ContainerElement"))
public typealias ContainingView = ContainerElement

@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2")) public typealias RecyclerView = Recycler2
@Deprecated("Renamed to Recycler2", ReplaceWith("Recycler2")) public typealias ViewPager = Recycler2
@Deprecated("Renamed to Frame", ReplaceWith("Frame")) public typealias Stack = Frame

@OptIn(ExperimentalContracts::class)
@Deprecated("Renamed to frame", ReplaceWith("frame(setup)"))
public inline fun ElementWriter.stack(setup: Frame.() -> Unit = {}): Frame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Frame(context) , setup)
}


@Deprecated("Renamed for consistency", ReplaceWith("dynamicWeight(amount)"))
public fun ElementWriter.CanAddWeight.changingWeight(amount: ReactiveContext.() -> Float): ElementWriter.CanAddShownWhen = dynamicWeight(amount)

@Deprecated("Renamed for consistency", ReplaceWith("dynamicSizeConstraints(constraints)"))
public fun ElementWriter.CanAddSizing.changingSizeConstraints(constraints: ReactiveContext.() -> SizeConstraints): ElementWriter.CanAddTheme = dynamicSizeConstraints(constraints)

@Deprecated("Use VideoView instead", ReplaceWith("VideoView"), level = DeprecationLevel.ERROR) public typealias Video = VideoView

@Deprecated("Import has moved", ReplaceWith("label(label, content)", "com.lightningkite.kiteui.views.l2.label"))
public inline fun ElementWriter.label(label: String, content: LinearLayoutElement.() -> Unit): LabeledView = l2Label(label, content)