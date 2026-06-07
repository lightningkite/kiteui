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

/**
 * Overrides how the underlying ZXing WebAssembly module is loaded. Must be called before the first
 * `detect()`. The `overrides` object accepts a `locateFile(path, prefix)` function returning the URL
 * to fetch the `.wasm` binary from. See [CameraScannerWasm].
 */
external fun setZXingModuleOverrides(overrides: dynamic)