package com.lightningkite.kiteui.views

import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
public actual suspend fun Element.driverScreenshot(): String {
    val view = native
    val bitmap = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    bitmap.recycle()
    return Base64.encode(stream.toByteArray())
}
