@file:JsModule("barcode-detector")
@file:JsNonModule

package com.lightningkite.kiteui.camera


import kotlin.js.Promise

external class BarcodeDetector(options: dynamic = definedExternally) {
    fun detect(image: dynamic): Promise<Array<DetectedBarcode>>

    companion object {
        fun getSupportedFormats(): Promise<Array<String>>
    }
}