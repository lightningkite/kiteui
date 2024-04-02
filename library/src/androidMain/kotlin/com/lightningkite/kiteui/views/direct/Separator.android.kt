package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import com.lightningkite.kiteui.models.px

import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.lparams


@ViewDsl
actual inline fun ViewWriter.separatorActual(crossinline setup: Separator.() -> Unit): Unit {
    val c = stack.asReversed().asSequence().filterIsInstance<SimplifiedLinearLayout>().firstOrNull()
        ?: throw IllegalStateException("Separators can only be used inside rows or columns")
    viewElement(factory = { NSeparator(it, c.orientation == SimplifiedLinearLayout.HORIZONTAL) }, wrapper = ::Separator) {
        handleTheme(native, foreground = { it, v ->
            v.background = ColorDrawable(it.foreground.closestColor().colorInt())
            v.alpha = 0.25f
            val size = it.outlineWidth.value.coerceAtLeast(1f).toInt()
            v.thickness = size
            (v.parent as? SimplifiedLinearLayout)?.let {
                v.lparams.run {
                    width = if(it.orientation == SimplifiedLinearLayout.HORIZONTAL) size else ViewGroup.LayoutParams.MATCH_PARENT
                    height = if(it.orientation == SimplifiedLinearLayout.HORIZONTAL) ViewGroup.LayoutParams.MATCH_PARENT else size
                }
            } ?: (v.parent as? DesiredSizeView)?.let {
                if(c.orientation == SimplifiedLinearLayout.HORIZONTAL) {
                    it.constraints = it.constraints.copy(width = size.px)
                } else {
                    it.constraints = it.constraints.copy(height = size.px)
                }
            }
        }) {
            native.minimumWidth = 1
            native.minimumHeight = 1
            setup(this)
        }
    }
}

actual class NSeparator(context: Context, val containerHorizontal: Boolean) : View(context) {
    var thickness: Int = 1
        set(value) {
            field = value
            requestLayout()
        }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(if(containerHorizontal) thickness else measuredWidth, if(!containerHorizontal) measuredHeight else thickness)
    }
}