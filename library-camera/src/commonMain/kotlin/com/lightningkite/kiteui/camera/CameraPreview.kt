// by Claude
package com.lightningkite.kiteui.camera

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.Untested
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.core.MutableReactive
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Barcode format types supported by the camera scanner.
 */
enum class BarcodeFormat {
    QR_CODE,
    CODE_128,
    CODE_39,
    CODE_93,
    EAN_8,
    EAN_13,
    UPC_A,
    UPC_E,
    PDF_417,
    DATA_MATRIX,
    AZTEC,
    ITF,
    CODABAR
}

/**
 * Result from a barcode scan.
 */
data class BarcodeResult(
    val rawValue: String,
    val format: BarcodeFormat,
    val timestamp: Long
)

/**
 * A camera preview view that supports barcode/QR code scanning and photo capture.
 *
 * Implementation is platform-specific:
 * - Android: Uses CameraX + ML Kit
 * - iOS: Uses AVFoundation + Vision
 * - JS: Uses getUserMedia + zxing-wasm
 *
 * Usage:
 * ```kotlin
 * cameraPreview {
 *     onBarcode { results ->
 *         // Handle scanned barcodes
 *     }
 * }
 * ```
 */
@ExperimentalKiteUi
@Untested
expect class CameraPreview(context: ElementContext) : NativeElement {
    /**
     * Captures a photo from the camera.
     * @return The captured image, or null if capture failed or permissions not granted.
     */
    suspend fun capture(): ImageLocal?

    /**
     * Registers a callback for barcode scan results.
     * @param formats The barcode formats to scan for. If empty, scans for common formats (QR, Code128, Code39, Code93).
     * @param action Callback invoked when barcodes are detected. Receives the list of results.
     */
    fun onBarcode(formats: Set<BarcodeFormat> = setOf(), action: (List<BarcodeResult>) -> Unit)

    /**
     * Whether camera permissions have been granted.
     * The view will automatically request permissions when added to the view hierarchy.
     */
    val hasPermissions: MutableReactive<Boolean>
}

/**
 * Creates a camera preview view for scanning barcodes and capturing photos.
 */
@OptIn(ExperimentalContracts::class)
@ExperimentalKiteUi
@Untested
inline fun ElementWriter.cameraPreview(setup: CameraPreview.() -> Unit = {}): CameraPreview {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(CameraPreview(context), setup)
}
