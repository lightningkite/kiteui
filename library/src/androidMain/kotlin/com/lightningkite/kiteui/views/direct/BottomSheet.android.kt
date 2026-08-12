package com.lightningkite.kiteui.views.direct

import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlin.coroutines.CoroutineContext

public actual fun ElementContext.openBottomSheet(
    halfScreenRatio: Float,
    dim: Boolean,
    view: ElementWriter.CanAddTheme.() -> Unit
) {
    val dialog = BottomSheetDialog(activity)
    dialog.behavior.halfExpandedRatio = halfScreenRatio
    val overlay = overlayFrame ?: return
    var createdView: Element? = null
    overlay.withoutAnimation {
        object : ViewWriter {
            override val coroutineContext: CoroutineContext = overlay.coroutineContext
            override val context: ElementContext get() = this@openBottomSheet

            @OverrideOnly
            override fun willAddChild(element: Element) {
                element.underlyingNativeElement.parent = overlay
                element.themeChoice = ThemeDerivation.Set(overlay.theme.let { it.revert ?: it }[DialogSemantic].theme)
            }

            @OverrideOnly
            override fun addChild(element: Element) {
                createdView = element
                dialog.setContentView(element.native)
            }
        }.popoverWriter {
            @OptIn(OverrideOnly::class)
            createdView?.onShutdown()
            dialog.dismiss()
        }.apply {
            col {
                centered.card.write(object : NativeElement(context) {
                    override val native: BottomSheetDragHandleView = BottomSheetDragHandleView(context.activity).apply {
                        minimumWidth = 5.rem.value.toInt()
                        minimumHeight = 1.rem.value.toInt()
                    }

                    override fun nativeApplyTheme(theme: ThemeAndBack) {
                        super.nativeApplyTheme(theme)
                        native.setImageDrawable(drawableWithoutCorners(theme.theme.icon, Color.transparent, 0.px))
                    }
                })
                view()
            }
        }
    }
    dialog.show()
    if (!dim) dialog.window?.setDimAmount(0f)
}