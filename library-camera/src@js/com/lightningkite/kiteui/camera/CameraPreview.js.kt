// by Claude
package com.lightningkite.kiteui.camera

import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.direct.Frame
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.w3c.dom.*
import org.w3c.dom.mediacapture.MediaStreamConstraints
import org.w3c.files.File
import org.w3c.files.FilePropertyBag
import kotlin.coroutines.resume
import kotlin.js.Promise

external interface DetectedBarcode {
    val rawValue: String
    val format: String
    val boundingBox: dynamic
    val cornerPoints: Array<dynamic>
}

/**
 * Configures where the ZXing WebAssembly binary used for barcode scanning is fetched from at runtime.
 *
 * By default the binary is loaded from the jsDelivr CDN, so downstream apps need no setup and bundle
 * nothing. Override [wasmUrl] (before the first scan) if a CDN is unavailable in your environment —
 * e.g. blocked by Content Security Policy, browser tracking protection, or when offline. Point it at
 * any host serving `zxing_reader.wasm`, such as your own static assets:
 *
 * ```kotlin
 * CameraScannerWasm.wasmUrl = "/assets/zxing_reader.wasm"
 * ```
 *
 * The default pins the version shipped with `barcode-detector`; bump it alongside that dependency.
 */
object CameraScannerWasm {
    var wasmUrl: String =
        "https://cdn.jsdelivr.net/npm/zxing-wasm@2.2.4/dist/reader/zxing_reader.wasm"
}

private var wasmOverrideApplied = false

private fun applyWasmOverride() {
    if (wasmOverrideApplied) return
    wasmOverrideApplied = true
    val url = CameraScannerWasm.wasmUrl
    val overrides: dynamic = js("({})")
    // Assigning a Kotlin lambda captures `url` in a real closure — safe under minification, unlike
    // referencing the local inside a js("...") string literal.
    overrides.locateFile = { _: String, _: String -> url }
    setZXingModuleOverrides(overrides)
}

actual class CameraPreview actual constructor(context: ElementContext) : NativeElement(context) {
    private var videoElement: HTMLVideoElement? = null
    private var canvasElement: HTMLCanvasElement? = null
    private var mediaStream: dynamic = null
    private var scanningJob: Job? = null
    private var barcodeCallback: ((List<BarcodeResult>) -> Unit)? = null
    private var requestedFormats: Set<BarcodeFormat> = setOf()
    private var loggedScanError = false

    private val _hasPermissions = Signal(false)
    actual val hasPermissions: MutableReactive<Boolean> get() = _hasPermissions

    init {
        native.tag = "div"
        native.classes.add("camera-preview")
        native.setStyleProperty("position", "relative")
        native.setStyleProperty("width", "100%")
        native.setStyleProperty("height", "100%")
        native.setStyleProperty("background-color", "black")

        native.onElement { container ->
            // Create video element
            val video = document.createElement("video") as HTMLVideoElement
            video.autoplay = true
            video.playsInline = true
            video.muted = true
            video.style.width = "100%"
            video.style.height = "100%"
            video.style.objectFit = "cover"
            container.appendChild(video)
            videoElement = video

            // Create hidden canvas for capture
            val canvas = document.createElement("canvas") as HTMLCanvasElement
            canvas.style.display = "none"
            container.appendChild(canvas)
            canvasElement = canvas

            // Start camera
            startCamera()
        }
    }

    @OptIn(OverrideOnly::class)
    override fun onShutdown() {
        super.onShutdown()
        stopCamera()
    }

    private fun startCamera() {
        val video = videoElement ?: return
        val constraints = js("{video: {facingMode: 'environment'}, audio: false}")

        window.navigator.mediaDevices.getUserMedia(constraints.unsafeCast<MediaStreamConstraints>())
            .then { stream: dynamic ->
                mediaStream = stream
                video.srcObject = stream
                _hasPermissions.value = true
                startScanning()
                Unit
            }
            .catch { error: dynamic ->
                console.error("Camera access denied:", error)
                _hasPermissions.value = false
                Unit
            }
    }

    private fun stopCamera() {
        scanningJob?.cancel()
        scanningJob = null

        // Note: can't use ?.let here — `mediaStream` is `dynamic`, so Kotlin/JS resolves `.let`
        // as a member call on the JS object rather than the stdlib extension, throwing
        // "tmp.let is not a function" at runtime. Use an explicit null check instead.
        val stream = mediaStream
        if (stream != null) {
            val tracks = stream.getTracks() as Array<dynamic>
            tracks.forEach { track ->
                track.stop()
            }
        }
        mediaStream = null
    }

    actual fun onBarcode(formats: Set<BarcodeFormat>, action: (List<BarcodeResult>) -> Unit) {
        requestedFormats = formats
        barcodeCallback = action
    }

    private fun startScanning() {
        if (barcodeCallback == null) return
        startBarcodeScanning()
    }

    private fun startBarcodeScanning() {
        val video = videoElement ?: return

        try {
            // Point ZXing at our configured WASM URL before its module first initializes (on first
            // detect). Without this the default backend CDN host is used, which some environments block.
            applyWasmOverride()

            val formats = if (requestedFormats.isEmpty()) {
                arrayOf("qr_code", "code_128", "code_39", "code_93")
            } else {
                requestedFormats.mapNotNull { it.toBarcodeDetectorFormat() }.toTypedArray()
            }

            // Use the polyfill which works on all browsers
            val detector = BarcodeDetector(js("({formats: formats})"))

            // Use the RView's coroutine scope which is cancelled on shutdown
            scanningJob = launch {
                while (isActive) {
                    delay(100) // Scan every 100ms

                    if (video.readyState >= 2) { // HAVE_CURRENT_DATA or higher
                        try {
                            val barcodes = detectBarcodes(detector, video)
                            if (barcodes.isNotEmpty()) {
                                barcodeCallback?.invoke(barcodes)
                            }
                        } catch (e: Exception) {
                            // Log once, not every frame — a persistent failure would otherwise spam
                            // the console 10x/second.
                            if (!loggedScanError) {
                                loggedScanError = true
                                console.error("Barcode scanning failed:", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            // Setup itself failed (e.g. detector construction or the WASM override). Without this
            // catch the throw is swallowed by the getUserMedia promise chain and nothing scans.
            console.error("Barcode scan setup failed:", e)
        }
    }

    private suspend fun detectBarcodes(detector: BarcodeDetector, video: HTMLVideoElement): List<BarcodeResult> =
        suspendCancellableCoroutine { continuation ->
            detector.detect(video)
                .then { results ->
                    val barcodeResults = results.mapNotNull { barcode ->
                        val format = barcode.format.fromBarcodeDetectorFormat() ?: return@mapNotNull null
                        // `js("Date.now()")` is a JS number = Kotlin Double. `as Long` would throw
                        // ClassCastException because Kotlin/JS Long is a boxed type, not a JS number.
                        BarcodeResult(barcode.rawValue, format, (js("Date.now()") as Double).toLong())
                    }
                    continuation.resume(barcodeResults)
                    Unit
                }
                .catch { error: dynamic ->
                    // Log once — a failed WASM load throws on every frame otherwise. This is the
                    // signal that distinguishes "detector broken" from "no barcode in frame".
                    if (!loggedScanError) {
                        loggedScanError = true
                        console.error("Barcode detect() rejected:", error)
                    }
                    continuation.resume(emptyList())
                    Unit
                }
        }

    actual suspend fun capture(): ImageLocal? = suspendCancellableCoroutine { continuation ->
        if (!_hasPermissions.value) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val video = videoElement
        val canvas = canvasElement

        if (video == null || canvas == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val width = video.videoWidth
        val height = video.videoHeight

        if (width == 0 || height == 0) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        canvas.width = width
        canvas.height = height

        val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
        ctx.drawImage(video, 0.0, 0.0, width.toDouble(), height.toDouble())

        canvas.toBlob({ blob ->
            if (blob != null) {
                val file = File(
                    fileBits = arrayOf(blob),
                    fileName = "capture_${js("Date.now()")}.jpg",
                    options = FilePropertyBag(type = "image/jpeg")
                )
                continuation.resume(ImageLocal(file))
            } else {
                continuation.resume(null)
            }
        }, "image/jpeg", 0.95)
    }
}

private fun BarcodeFormat.toBarcodeDetectorFormat(): String? = when (this) {
    BarcodeFormat.QR_CODE -> "qr_code"
    BarcodeFormat.CODE_128 -> "code_128"
    BarcodeFormat.CODE_39 -> "code_39"
    BarcodeFormat.CODE_93 -> "code_93"
    BarcodeFormat.EAN_8 -> "ean_8"
    BarcodeFormat.EAN_13 -> "ean_13"
    BarcodeFormat.UPC_A -> "upc_a"
    BarcodeFormat.UPC_E -> "upc_e"
    BarcodeFormat.PDF_417 -> "pdf417"
    BarcodeFormat.DATA_MATRIX -> "data_matrix"
    BarcodeFormat.AZTEC -> "aztec"
    BarcodeFormat.ITF -> "itf"
    BarcodeFormat.CODABAR -> "codabar"
}

private fun String.fromBarcodeDetectorFormat(): BarcodeFormat? = when (this) {
    "qr_code" -> BarcodeFormat.QR_CODE
    "code_128" -> BarcodeFormat.CODE_128
    "code_39" -> BarcodeFormat.CODE_39
    "code_93" -> BarcodeFormat.CODE_93
    "ean_8" -> BarcodeFormat.EAN_8
    "ean_13" -> BarcodeFormat.EAN_13
    "upc_a" -> BarcodeFormat.UPC_A
    "upc_e" -> BarcodeFormat.UPC_E
    "pdf417" -> BarcodeFormat.PDF_417
    "data_matrix" -> BarcodeFormat.DATA_MATRIX
    "aztec" -> BarcodeFormat.AZTEC
    "itf" -> BarcodeFormat.ITF
    "codabar" -> BarcodeFormat.CODABAR
    else -> null
}
