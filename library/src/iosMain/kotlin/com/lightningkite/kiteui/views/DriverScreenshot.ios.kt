package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class, ExperimentalForeignApi::class)
actual suspend fun RView.driverScreenshot(): String {
    val view = native
    val bounds = view.bounds
    UIGraphicsBeginImageContextWithOptions(
        bounds.useContents { CGSizeMake(size.width, size.height) },
        false,
        0.0
    )
    val graphicsContext = UIGraphicsGetCurrentContext() ?: run {
        UIGraphicsEndImageContext()
        throw DriverActionException("Failed to create graphics context for screenshot")
    }
    view.layer.renderInContext(graphicsContext)
    val image = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    val pngData = image?.let { UIImagePNGRepresentation(it) }
        ?: throw DriverActionException("Failed to capture screenshot")
    return Base64.encode(pngData.toByteArray())
}
