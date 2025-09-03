package com.lightningkite.kiteui.views

import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Rect
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.views.withoutAnimation

actual val RView.areAnimationsEnabled: Boolean get() = false
actual inline fun RView.withoutAnimation(action: () -> Unit) {
    action()
}


actual abstract class RView actual constructor(context: RContext) : RViewHelper(context) {

    actual override var showOnPrint: Boolean
        get() {
            TODO()
        }
        set(value) {}

    actual override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {
        TODO()
    }

    actual override fun requestFocus() {
        TODO("Not yet implemented")
    }

    actual override fun screenRectangle(): Rect? {
        TODO()
    }

    actual override fun applyTheme(theme: ThemeAndBack) {

    }

    actual override fun internalAddChild(index: Int, view: RView) {

    }

    actual override fun internalRemoveChild(index: Int) {

    }

    actual override fun internalClearChildren() {}


    @Composable
    abstract fun compose(): Unit
}