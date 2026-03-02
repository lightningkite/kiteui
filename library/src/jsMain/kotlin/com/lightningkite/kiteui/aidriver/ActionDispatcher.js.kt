// by Claude - JS/Web action dispatcher: handles screenshots via modern-screenshot, delegates rest to shared impl
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.views.RView
import kotlinx.browser.document
import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun dispatchAction(
    action: UiAction,
    root: RView,
    navigator: PageNavigator?
): ActionDispatchResult {
    if (action is UiAction.Screenshot) {
        return try {
            // by Claude - dynamically import modern-screenshot (Vite handles ESM natively)
            val mod = js("import('modern-screenshot')").unsafeCast<Promise<dynamic>>().await()
            val body = document.body ?: return ActionDispatchResult(false, "No document body")
            // domToPng returns a data URL: "data:image/png;base64,..."
            val dataUrl = (mod.domToPng(body) as Promise<String>).await()
            val prefix = "data:image/png;base64,"
            if (!dataUrl.startsWith(prefix)) {
                return ActionDispatchResult(false, "Unexpected data URL format")
            }
            val base64Str = dataUrl.removePrefix(prefix)
            val bytes = Base64.Default.decode(base64Str)
            ActionDispatchResult(true, bytes = bytes)
        } catch (e: Exception) {
            ActionDispatchResult(false, "Screenshot failed: ${e.message}")
        }
    }
    return dispatchActionImpl(action, root, navigator)
}
