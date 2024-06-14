package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.views.NView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.jvm.JvmInline
import kotlin.contracts.*

expect class NImageView : NView

@JvmInline
value class ImageView(override val native: NImageView) : RView<NImageView>

@ViewDsl
expect fun ViewWriter.imageActual(setup: ImageView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.image(noinline setup: ImageView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; imageActual(setup) }
expect var ImageView.source: ImageSource?
expect var ImageView.scaleType: ImageScaleType
expect var ImageView.description: String?
expect var ImageView.refreshOnParamChange: Boolean

@ViewDsl
expect fun ViewWriter.zoomableImageActual(setup: ImageView.()->Unit = {}): Unit
@OptIn(ExperimentalContracts::class) @ViewDsl inline fun ViewWriter.zoomableImage(noinline setup: ImageView.() -> Unit = {}) { contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }; zoomableImageActual(setup) }