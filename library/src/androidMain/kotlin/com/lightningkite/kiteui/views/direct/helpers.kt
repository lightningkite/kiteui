package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import com.lightningkite.kiteui.views.*
import java.lang.ref.WeakReference
import java.util.WeakHashMap


//inline fun <T: NView, W: RView<T>> ViewWriter.viewElement(
//    factory: (Context) -> T,
//    wrapper: (T) -> W,
//    crossinline setup: W.() -> Unit,
//) {
//    val native = factory(context)
//    val wrapped = wrapper(native)
//    element(native) {
//        setup(wrapped)
//    }
//}
