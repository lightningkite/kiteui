// by Claude
package com.lightningkite.kiteui.views.l2

import android.view.ViewGroup
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.RView

/**
 * Android implementation of sizeConstraints for RView.
 * Sets layout params and minimum sizes on the native view.
 * by Claude
 */
actual fun RView.setSizeConstraints(
    width: Dimension?,
    height: Dimension?,
    minWidth: Dimension?,
    maxWidth: Dimension?,
    minHeight: Dimension?,
    maxHeight: Dimension?
) {
    val view = this.native
    val params = view.layoutParams ?: ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    width?.let { params.width = it.px.toInt() }
    height?.let { params.height = it.px.toInt() }
    minWidth?.let { view.minimumWidth = it.px.toInt() }
    minHeight?.let { view.minimumHeight = it.px.toInt() }

    // Note: maxWidth and maxHeight require DesiredSizeView wrapper in the full implementation
    // For spacers in Recycler3, we only need width/height which works with layout params

    view.layoutParams = params
    view.requestLayout()
}

/**
 * Android implementation of setTranslation for RView.
 * Uses view.translationX/Y which doesn't trigger layout events.
 * by Claude
 */
actual fun RView.setTranslation(x: Double, y: Double) {
    native.translationX = x.toFloat()
    native.translationY = y.toFloat()
}

/** No-op on Android - native views don't have CSS transitions. by Claude */
actual fun RView.disableTransformTransition() {}
