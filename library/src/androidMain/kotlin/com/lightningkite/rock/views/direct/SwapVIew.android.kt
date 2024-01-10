package com.lightningkite.rock.views.direct

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.lightningkite.rock.models.ScreenTransition
import com.lightningkite.rock.views.ViewDsl
import com.lightningkite.rock.views.ViewWriter
import com.lightningkite.rock.views.lparams
import com.lightningkite.rock.views.shutdown
import java.util.WeakHashMap

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NSwapView(context: Context): FrameLayout(context) {
    lateinit var viewWriter: ViewWriter
}

@ViewDsl
actual fun ViewWriter.swapView(setup: SwapView.() -> Unit) {
    return viewElement(factory = ::NSwapView, wrapper = ::SwapView, setup = {
        native.viewWriter = newViews()
        setup(this)
    })
}

@ViewDsl
actual fun ViewWriter.swapViewDialog(setup: SwapView.() -> Unit) {

}

actual fun SwapView.swap(
    transition: ScreenTransition,
    createNewView: ViewWriter.() -> Unit,
) {
    val oldView = this.native.getChildAt(0)
    native.viewWriter.rootCreated = null
    native.viewWriter.createNewView()
    val newView = native.viewWriter.rootCreated
    newView?.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,  ViewGroup.LayoutParams.MATCH_PARENT)
    TransitionManager.beginDelayedTransition(native, TransitionSet().apply {
        oldView?.let { transition.exit?.addTarget(it) }
        newView?.let { transition.enter?.addTarget(it) }
        transition.enter?.let { addTransition(it) }
        transition.exit?.let { addTransition(it) }
    })
    oldView?.let { oldNN -> native.removeView(oldNN); oldNN.shutdown() }
    newView?.let { native.addView(it) }
}