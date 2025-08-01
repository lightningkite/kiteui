package com.lightningkite.kiteui.views.direct

import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.Shape
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.NewViewWriter
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.backgroundDrawableWithoutCorners
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.drawableWithoutCorners
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.popoverWriter
import com.lightningkite.kiteui.views.withoutAnimation
import kotlin.coroutines.CoroutineContext

public actual fun ViewWriter.openBottomSheet(
    halfScreenRatio: Float,
    dim: Boolean,
    view: ViewWriter.() -> ViewModifiable
) {
    val dialog = BottomSheetDialog(context.activity)
    dialog.behavior.halfExpandedRatio = halfScreenRatio
    val o = overlayFrame ?: return
    var createdView: RView? = null
    o.withoutAnimation {
        object: ViewWriter() {
            public override val context: RContext get() = this@openBottomSheet.context
            public override fun willAddChild(view: RView) {
                println("willAddChild $view: ${o.theme}")
                view.parent = o
                view.themeChoice = ThemeDerivation.Set(o.theme.let { it.revert ?: it }[DialogSemantic].theme)
            }
            public override fun addChild(view: RView) {
                println("Adding child $view")
                createdView = view
                dialog.setContentView(view.native)
            }
            public override val coroutineContext: CoroutineContext = o.coroutineContext
        }.popoverWriter {
            createdView?.shutdown()
            dialog.dismiss()
        }.apply {
            col {
                centered - card - write(object: RView(context) {
                    override val native: BottomSheetDragHandleView = BottomSheetDragHandleView(context.activity).apply {
                        minimumWidth = 5.rem.value.toInt()
                        minimumHeight = 1.rem.value.toInt()
                    }
                    override fun applyTheme(theme: ThemeAndBack) {
                        super.applyTheme(theme)
                        println("Theme is ${theme.theme.id}")
                        native.setImageDrawable(drawableWithoutCorners(theme.theme.icon, Color.transparent, 0.px).apply {
//                            this.
//                            minimumWidth = 5.rem.value.toInt()
//                            minimumHeight = 1.rem.value.toInt()
                        })
                    }
                }) {
                }
                view()
            }
        }
    }
    dialog.show()
    if(!dim) dialog.window?.setDimAmount(0f)
}