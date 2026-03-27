package com.lightningkite.kiteui.testing

import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Takes a screenshot via the driver, decodes the base64 PNG, and writes it to [filePath].
 * Returns the absolute path of the written file.
 */
@OptIn(ExperimentalEncodingApi::class)
suspend fun UiTestScope.saveScreenshot(filePath: String, target: String = "root"): String {
    val base64 = screenshot(target)
    val file = File(filePath)
    file.parentFile?.mkdirs()
    file.writeBytes(Base64.decode(base64))
    return file.absolutePath
}
