// by Claude - Android action dispatcher: handles screenshots via View.draw, delegates rest to shared impl
package com.lightningkite.kiteui.aidriver

import android.graphics.Bitmap
import android.graphics.Canvas
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import java.io.ByteArrayOutputStream

actual suspend fun dispatchAction(
    action: UiAction,
    root: RView,
    navigator: PageNavigator?
): ActionDispatchResult {
    if (action is UiAction.Screenshot) {
        return try {
            // by Claude - capture the Android view hierarchy as a PNG
            val view = root.native
            val width = view.width
            val height = view.height
            if (width <= 0 || height <= 0) {
                return ActionDispatchResult(false, "View has no size: ${width}x${height}")
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            bitmap.recycle()
            ActionDispatchResult(true, bytes = out.toByteArray())
        } catch (e: Exception) {
            ActionDispatchResult(false, "Screenshot failed: ${e.message}")
        }
    }
    return dispatchActionImpl(action, root, navigator)
}
