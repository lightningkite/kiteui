package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Angle
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.reactive.CalculationContext


/**
 * A native view in the underlying view system.
 */
expect open class NView

expect class NContext
expect val NView.nContext: NContext
expect val NContext.darkMode: Boolean?

expect val NView.calculationContext: CalculationContext
expect var NView.nativeRotation: Angle
expect var NView.opacity: Double
expect var NView.exists: Boolean
expect var NView.visible: Boolean
expect var NView.spacing: Dimension
expect var NView.ignoreInteraction: Boolean
expect fun NView.clearNViews()
expect fun NView.addNView(child: NView)
expect fun NView.removeNView(child: NView)
expect fun NView.listNViews(): List<NView>
expect fun NView.scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean = true)
expect fun NView.consumeInputEvents()
//expect fun NView.getFrameRelativeToParent(): Rect

expect inline fun NView.withoutAnimation(action: ()->Unit)
