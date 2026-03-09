// by Claude - iOS action dispatcher: handles screenshots via UIGraphicsContext, delegates rest to shared impl
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.*
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
actual suspend fun dispatchAction(
    action: UiAction,
    root: RView,
    navigator: PageNavigator?
): ActionDispatchResult {
    if (action is UiAction.Screenshot) {
        return try {
            // by Claude - capture the iOS view hierarchy as a PNG
            val view = root.native
            val bounds = view.bounds
            val width = bounds.useContents { size.width }
            val height = bounds.useContents { size.height }
            if (width <= 0.0 || height <= 0.0) {
                return ActionDispatchResult(false, "View has no size: ${width}x${height}")
            }
            val size = CGSizeMake(width, height)
            UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
            val context = UIGraphicsGetCurrentContext()
                ?: return ActionDispatchResult(false, "Failed to create graphics context")
            view.layer.renderInContext(context)
            val image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            if (image == null) {
                return ActionDispatchResult(false, "Failed to capture image")
            }
            val pngData = UIImagePNGRepresentation(image)
                ?: return ActionDispatchResult(false, "Failed to encode PNG")
            val length = pngData.length.toInt()
            val byteArray = ByteArray(length)
            byteArray.usePinned { pinned ->
                memcpy(pinned.addressOf(0), pngData.bytes, pngData.length)
            }
            ActionDispatchResult(true, bytes = byteArray)
        } catch (e: Exception) {
            ActionDispatchResult(false, "Screenshot failed: ${e.message}")
        }
    }
    return dispatchActionImpl(action, root, navigator)
}
