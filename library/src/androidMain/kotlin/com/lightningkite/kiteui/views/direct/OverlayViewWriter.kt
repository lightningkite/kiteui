package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.graphics.PixelFormat
import android.view.WindowManager
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.native
import kotlin.coroutines.CoroutineContext

class OverlayViewWriter(override val context: ElementContext,
                        override val coroutineContext: CoroutineContext
) : ViewWriter {

    val windowManager = context.activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    @OverrideOnly
    override fun willAddChild(element: Element) {
        windowManager.addView(element.native, WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ))
    }

    @OverrideOnly
    override fun addChild(element: Element) {
        windowManager.removeView(element.native)
    }
}