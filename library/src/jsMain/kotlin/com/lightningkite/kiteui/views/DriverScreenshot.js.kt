package com.lightningkite.kiteui.views

import kotlinx.coroutines.await
import kotlin.js.Promise

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
actual suspend fun RView.driverScreenshot(): String {
    val mod = js("import('modern-screenshot')").unsafeCast<Promise<dynamic>>().await()
    val element = this.native.element ?: throw DriverActionException("Element not yet attached to DOM")
    val dataUrl = (mod.domToPng(element) as Promise<String>).await()
    val prefix = "data:image/png;base64,"
    if (!dataUrl.startsWith(prefix)) {
        throw DriverActionException("Unexpected screenshot data URL format")
    }
    return dataUrl.removePrefix(prefix)
}
