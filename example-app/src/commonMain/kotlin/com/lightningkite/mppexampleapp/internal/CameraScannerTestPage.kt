// by Claude
package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.camera.BarcodeFormat
import com.lightningkite.kiteui.camera.BarcodeResult
import com.lightningkite.kiteui.camera.cameraPreview
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal

@Routable("/camera-scanner-test")
object CameraScannerTestPage : Page {
    override val title: Reactive<String> = Constant("Camera Scanner Test")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val scannedBarcodes = Signal<List<BarcodeResult>>(emptyList())
        val captureCount = Signal(0)
        val hasPermission = Signal(false)

        col {
            h1 { content = "Camera & Barcode Scanner" }

            text { content = "Point the camera at a QR code or barcode to scan it." }

            // Camera preview container
            sizeConstraints(height = 20.rem).frame {
                val preview = cameraPreview {
                    onBarcode(setOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128, BarcodeFormat.EAN_13)) { results ->
                        scannedBarcodes.value = results
                    }
                }

                // Bind permission state
                reactive {
                    hasPermission.value = preview.hasPermissions.invoke()
                }
            }

            // Permission status
            text {
                ::content {
                    if (hasPermission.invoke()) "✓ Camera permission granted"
                    else "Waiting for camera permission..."
                }
            }

            separator()

            // Scanned results section
            h2 { content = "Scanned Barcodes" }

            col {
                reactive {
                    val barcodes = scannedBarcodes.invoke()
                    clearChildren()
                    if (barcodes.isEmpty()) {
                        text { content = "No barcodes scanned yet" }
                    } else {
                        for (barcode in barcodes) {
                            card.col {
                                row {
                                    bold.text { content = barcode.format.name }
                                    expanding.text { content = "" }
                                    text { content = "TS: ${barcode.timestamp}" }
                                }
                                text {
                                    content = barcode.rawValue
                                }
                            }
                        }
                    }
                }
            }

            separator()

            // Capture button
            row {
                important.button {
                    text { content = "Capture Photo" }
                    onClick {
                        captureCount.value = captureCount.value + 1
                    }
                }
            }

            // Last capture info
            text {
                ::content {
                    val count = captureCount.invoke()
                    if (count > 0) "Photos captured: $count" else "No photo captured yet"
                }
            }

            separator()

            // Supported formats info
            h3 { content = "Supported Formats" }
            text {
                content = BarcodeFormat.entries.joinToString(", ") { it.name }
            }
        }
    }
}
