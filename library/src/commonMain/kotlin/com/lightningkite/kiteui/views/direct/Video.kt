package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NVideo : NView

@JvmInline
value class Video(override val native: NVideo) : RView<NVideo>

@ViewDsl
expect fun ViewWriter.videoActual(setup: Video.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.video(noinline setup: Video.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; videoActual(setup) }
expect var Video.source: VideoSource?
expect val Video.time: Writable<Double>
expect val Video.playing: Writable<Boolean>
expect val Video.volume: Writable<Float>
expect var Video.showControls: Boolean
expect var Video.loop: Boolean
expect var Video.scaleType: ImageScaleType