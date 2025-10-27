package com.lightningkite.kiteui.views.direct

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual class SwapView actual constructor(context: RContext) :
    RView(context) {

    private val currentView = Signal<ViewModifiable?>(null)

    actual fun swap(
        transition: ScreenTransition,
        createNewView: ViewWriter.() -> ViewModifiable?
    ) {
        val writer = object : ViewWriter() {
            override val context: RContext get() = this@SwapView.context
            override val coroutineContext: CoroutineContext = Dispatchers.Main
            override fun addChild(view: RView) {
                if (view is ViewModifiable) {
                    currentView.value = view
                }
            }
        }
        writer.createNewView()
    }

    @Composable
    override fun compose() {
        val view = currentView.collectAsMutableState()
        Crossfade(targetState = view.value, modifier = Modifier) {
            it?.rView?.compose()
        }
    }
}